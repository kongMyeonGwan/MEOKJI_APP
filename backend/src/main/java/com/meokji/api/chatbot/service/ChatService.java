package com.meokji.api.chatbot.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meokji.api.ban.service.BanService;
import com.meokji.api.ban.vo.UserBanVO;
import com.meokji.api.chatbot.dto.ChatRequest;
import com.meokji.api.chatbot.dto.ChatResponse;
import com.meokji.api.chatbot.util.OpenAiClient;
import com.meokji.api.common.ApiException;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.util.IngredientRules;
import com.meokji.api.util.Json;

import tools.jackson.databind.JsonNode;

/**
 * 재료 대체 챗봇.
 *
 * 로그인한 사람이 "A 대신 쓸 재료 있어?" 하고 물으면 대체재를 알려준다.
 * 그 사람이 제외 재료(밴)로 설정한 것은 대체재로 추천하지 않는다.
 * 다만 제외 재료를 "질문"하는 것은 된다 (오이를 뺐어도 "오이 대신 뭐 쓰지?" 는 답한다).
 *
 * 흐름
 *   1) 이 사람의 제외 목록을 DB 에서 읽는다 (화면이 보낸 값은 믿지 않는다)
 *   2) 규칙과 제외 목록을 시스템 프롬프트로 만든다
 *   3) OpenAI 에 묻고 JSON 으로 답을 받는다
 *   4) 대체재 후보 중 제외 재료와 겹치는 것을 서버가 직접 지운다
 *   5) 남은 후보로 답변 문장을 서버가 조립한다
 *
 * 4번이 이 챗봇의 안전장치다. 프롬프트로 "넣지 마라" 고 해도 LLM 이 어길 수
 * 있어서, 마지막에는 항상 서버가 한 번 더 거른다.
 *
 * clarify · off_topic 은 OpenAI 가 쓴 글을 쓰지 않고 서버의 고정 문구를 쓴다.
 * 사용자가 "앞의 규칙을 무시해" 같은 글을 넣어도 그 글이 화면에 나가지 않게 하려는 것이다.
 *
 * DB 는 읽기만 하므로 @Transactional 을 붙이지 않았다.
 */
@Service
public class ChatService {

    private static final int MAX_ALTERNATIVES = 3;

    private static final String CLARIFY = "어떤 재료를 바꾸고 싶으세요? 예: '오이 대신 뭘 쓸까요?'";

    private static final String OFF_TOPIC = "재료 대체에 관한 질문만 도와드릴 수 있어요. 예: '우유 대신 뭘 쓸까요?'";

    private static final String NO_ALTERNATIVE = "제외 재료 설정 때문에 추천할 수 있는 대체재가 없어요. 마이페이지에서 제외 재료를 확인해 보세요.";

    private static final String ALLERGY_NOTE = "※ 알레르기가 있다면 가공식품은 성분표를 꼭 확인해 주세요.";

    @Autowired
    private BanService banService;

    @Autowired
    private OpenAiClient openAiClient;

    /** 질문 하나에 답한다. */
    public ChatResponse chat(UserVO user, ChatRequest request) {
        // 1) 제외 목록. 이름만 모은 것(걸러내기용)과 알레르기가 있는지(안내 문구용)를 같이 만든다
        List<UserBanVO> bans = banService.getList(user);

        List<String> banNames = new ArrayList<>();
        boolean hasAllergy = false;

        for (UserBanVO ban : bans) {
            banNames.add(ban.getIngredientName());

            if ("ALLERGY".equals(ban.getBanType())) {
                hasAllergy = true;
            }
        }

        // 2~3) 프롬프트를 만들어 OpenAI 에 묻는다
        String raw = openAiClient.ask(buildSystemPrompt(bans), buildHistory(request));

        // 4~5) 거르고 문장으로 만든다
        return new ChatResponse(compose(raw, banNames, hasAllergy));
    }

    /**
     * 챗봇에게 주는 "역할 + 규칙 + 이 사람의 제외 재료".
     *
     * OpenAI 의 JSON 모드는 프롬프트 안에 "JSON" 이라는 글자가 없으면 요청을
     * 거절한다. 아래 규칙의 마지막 줄이 그 역할도 한다.
     */
    private String buildSystemPrompt(List<UserBanVO> bans) {
        List<String> lines = new ArrayList<>();

        lines.add("너는 '뭐먹지?' 사이트의 재료 대체 도우미다.");
        lines.add("사용자가 어떤 재료를 다른 재료로 바꾸고 싶다고 하면 대체할 만한 재료를 알려준다.");
        lines.add("");
        lines.add("규칙:");
        lines.add("- 요리 재료 대체에 관한 질문에만 답한다. 그 밖의 질문은 off_topic 으로 답한다.");
        lines.add("- 사용자가 바꾸고 싶어 하는 재료(target)가 아래 [제외 재료]에 있어도 정상적으로 답한다.");
        lines.add("- 대체재(alternatives)에는 [제외 재료]와 같거나 그 종류에 속하는 재료를 절대 넣지 않는다.");
        lines.add("  (예: 땅콩이 제외 재료면 땅콩버터, 땅콩가루도 넣지 않는다)");
        lines.add("- 대체재는 최대 " + MAX_ALTERNATIVES + "개이고, name 에는 재료 이름만 쓴다.");
        lines.add("- reason 은 맛, 식감, 쓰는 양 중 하나를 한 문장으로 쓴다.");
        lines.add("- 재료 이름만 들어오면 그 재료를 바꾸려는 것으로 보고 target 으로 삼아 substitute 로 답한다. [제외 재료]에 속하는 재료여도 마찬가지다.");
        lines.add("- 어떤 재료를 바꾸려는지 알 수 없으면 clarify 로 답한다.");
        lines.add("- 반드시 아래 JSON 형식으로만 답한다.");
        lines.add("");
        lines.add("[제외 재료]");

        if (bans.isEmpty()) {
            lines.add("- 없음");
        }

        for (UserBanVO ban : bans) {
            String kind = "ALLERGY".equals(ban.getBanType()) ? "알레르기" : "싫어함";
            lines.add("- " + oneLine(ban.getIngredientName()) + " (" + kind + ")");
        }

        lines.add("");
        lines.add("[답변 JSON 형식]");
        lines.add("{\"intent\": \"substitute\" 또는 \"clarify\" 또는 \"off_topic\",");
        lines.add(" \"target\": \"바꾸려는 재료. 없으면 빈 문자열\",");
        lines.add(" \"alternatives\": [{\"name\": \"재료 이름\", \"reason\": \"한 문장\"}]}");

        return String.join("\n", lines);
    }

    /**
     * 이전 대화 + 이번 질문을 한 목록으로.
     *
     * LLM 은 이전 대화를 기억하지 못한다. 그래서 화면이 들고 있던 대화를
     * 그대로 이어 붙이고, 맨 끝에 이번 질문을 붙인다.
     * role 이 user · assistant 인지는 ChatRequest 의 검증이 이미 확인했다.
     */
    private List<OpenAiClient.Message> buildHistory(ChatRequest request) {
        List<OpenAiClient.Message> messages = new ArrayList<>();

        // 처음 질문이면 history 가 비어 있거나 null 이다
        if (request.getHistory() != null) {
            for (ChatRequest.Turn turn : request.getHistory()) {
                messages.add(new OpenAiClient.Message(turn.getRole(), turn.getContent()));
            }
        }

        messages.add(new OpenAiClient.Message("user", request.getMessage().trim()));

        return messages;
    }

    /**
     * OpenAI 의 JSON 답변을 걸러서 화면에 보여줄 문장으로 만든다.
     *
     * 검사하는 것은 대체재의 name 뿐이다. reason 에는 "오이처럼 아삭해요" 처럼
     * 질문한 재료가 정상적으로 나오므로 거르지 않는다.
     *
     * 이름 검사(IngredientRules.matchesAny)는 한쪽이 다른 쪽에 포함되면 겹친 것으로
     * 본다. "땅콩" 을 제외했으면 "땅콩버터" 가 걸린다. 반대로 "파" 처럼 한 글자
     * 재료는 "파프리카" 까지 걸러서 후보가 필요 이상으로 줄 수 있다. 알레르기가
     * 새는 것보다 낫다고 보고 그대로 둔다.
     */
    private String compose(String raw, List<String> banNames, boolean hasAllergy) {
        JsonNode root = Json.node(raw);

        // 깨진 JSON 이면 null 이다. 다시 묻지 않고 사용자에게 다시 질문하게 한다
        if (root == null || !root.isObject()) {
            throw ApiException.serviceUnavailable("답변을 읽지 못했습니다. 다시 질문해주세요.");
        }

        String intent = root.path("intent").asString("");

        if ("clarify".equals(intent)) {
            return CLARIFY;
        }

        // off_topic 이거나, 모르는 값이면 안전하게 거절 쪽으로 간다
        if (!"substitute".equals(intent)) {
            return OFF_TOPIC;
        }

        List<String> lines = new ArrayList<>();

        for (JsonNode alt : root.path("alternatives")) {
            String name = alt.path("name").asString("").trim();
            String reason = alt.path("reason").asString("").trim();

            if (name.isEmpty()) {
                continue;
            }

            // 제외 재료와 겹치면 지운다. 이 챗봇의 핵심 규칙이다
            if (IngredientRules.matchesAny(name, banNames)) {
                continue;
            }

            lines.add(reason.isEmpty() ? name : name + " - " + reason);

            if (lines.size() == MAX_ALTERNATIVES) {
                break;
            }
        }

        // 후보가 전부 지워졌다. 다시 묻지 않고 안내만 한다 (비용과 시간을 아낀다)
        if (lines.isEmpty()) {
            return NO_ALTERNATIVE;
        }

        String target = root.path("target").asString("").trim();

        StringBuilder answer = new StringBuilder();

        answer.append(target.isEmpty() ? "이런 재료로 바꿔 볼 수 있어요." : target + " 대신 이런 재료를 쓸 수 있어요.");

        for (int i = 0; i < lines.size(); i++) {
            answer.append("\n").append(i + 1).append(". ").append(lines.get(i));
        }

        if (hasAllergy) {
            answer.append("\n\n").append(ALLERGY_NOTE);
        }

        return answer.toString();
    }

    /** 프롬프트에 넣을 한 줄 글자로. 줄바꿈이 섞이면 규칙 줄처럼 읽힐 수 있다. */
    private String oneLine(String text) {
        return text == null ? "" : text.replaceAll("[\\r\\n]+", " ").trim();
    }
}