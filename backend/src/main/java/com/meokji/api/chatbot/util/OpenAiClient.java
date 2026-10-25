package com.meokji.api.chatbot.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.meokji.api.common.ApiException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * OpenAI 에 질문하고 답변 글자만 돌려준다.
 *
 * WeatherClient 처럼 JDK 의 HttpClient 를 쓴다. 부르는 곳이 한 군데고
 * 동기 호출이라, 의존성을 늘려 얻을 것이 없다.
 *
 * 하는 일은 딱 하나다.
 *     시스템 프롬프트 + 지금까지의 대화  ->  OpenAI  ->  답변(JSON 글자)
 * 프롬프트를 만드는 일과 답변을 해석하는 일은 ChatService 가 한다.
 *
 * 설정은 application.yml 의 app.openai.*
 *   api-key     비어 있으면 챗봇만 "준비 안 됨" 이 된다. 서버는 정상으로 뜬다.
 *   model       모델 이름. 안 되는 이름이면 로그에 model_not_found 가 찍힌다.
 *   max-tokens  답변 최대 길이(토큰). 비용과 답변 길이를 함께 제한한다.
 *
 * 에러는 ApiException 으로 던진다. ApiExceptionHandler 가 받아서
 * {"detail": 문장} 으로 응답하므로 화면은 그 문장만 보여주면 된다.
 */
@Slf4j
@Component
public class OpenAiClient {

    @Value("${app.openai.chat-url:https://api.openai.com/v1/chat/completions}")
    private String chatUrl;

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Value("${app.openai.api-key}")
    private String apiKey;

    @Value("${app.openai.model}")
    private String model;

    @Value("${app.openai.max-tokens}")
    private int maxTokens;

    private HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 대화 한 줄.
     *
     * role 은 "user"(사용자) 또는 "assistant"(챗봇) 이다.
     * 시스템 프롬프트는 이 목록에 넣지 않고 ask 의 첫 번째 값으로 따로 준다.
     */
    public record Message(String role, String content) {
    }

    /**
     * OpenAI 에 질문한다.
     *
     * 답변은 JSON 객체 글자로 받는다(response_format = json_object).
     * 이 모드는 시스템 프롬프트 안에 "JSON" 이라는 글자가 없으면 OpenAI 가
     * 400 으로 거절한다. 그러니 프롬프트에 JSON 으로 답하라고 꼭 적어야 한다.
     *
     * @param systemPrompt 역할 지시 + 이 사람의 제외 목록
     * @param history      지금까지의 대화 (마지막 줄이 이번 질문)
     * @return 답변 JSON 글자. ChatService 가 해석한다.
     * @throws ApiException 키가 없거나, 연결에 실패했거나, OpenAI 가 거절했을 때
     */
    public String ask(String systemPrompt, List<Message> history) {
        if (apiKey == null || apiKey.isBlank()) {
            // 키가 없는 팀원도 서버는 떠야 하니 시작할 때가 아니라 부를 때 알려준다
            throw ApiException.serviceUnavailable("챗봇이 아직 준비되지 않았습니다.");
        }

        String body = buildBody(systemPrompt, history);

        HttpResponse<String> response;

        try {
            response = http.send(
                    HttpRequest.newBuilder(URI.create(chatUrl))
                            // 답변을 만드는 데 몇 초 걸린다. 너무 길게 기다리지는 않는다
                            .timeout(Duration.ofSeconds(30))
                            .header("Content-Type", "application/json")
                            // 복사할 때 끝에 딸려 온 공백이 있으면 인증에 실패하니 자른다
                            .header("Authorization", "Bearer " + apiKey.trim())
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            throw ApiException.serviceUnavailable("챗봇 호출이 중단되었습니다. 다시 시도해주세요.");
        } catch (Exception exc) {
            // 연결 실패 · 시간 초과 · DNS 오류
            log.warn("OpenAI 에 연결하지 못했습니다 ({}).", exc.getClass().getSimpleName());
            throw ApiException.serviceUnavailable("챗봇에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }

        int status = response.statusCode();


        if (status != 200) {
            throw failure(status, response.body());
        }

        return contentOf(response.body());
    }

    /** OpenAI 에 보낼 요청 본문. */
    private String buildBody(String systemPrompt, List<Message> history) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", maxTokens);
        // 답변을 JSON 객체로 받는다. 서버가 후보를 하나씩 걸러내려면 글이 아니라 구조가 필요하다
        root.putObject("response_format").put("type", "json_object");

        ArrayNode messages = root.putArray("messages");

        // LLM 은 이전 대화를 기억하지 못한다.
        // 그래서 매번 [시스템 프롬프트] + [지금까지의 대화] 를 통째로 보낸다
        messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt);

        for (Message message : history) {
            // 화면이 보낸 role 을 그대로 믿지 않는다.
            // "system" 을 끼워 넣어 제외 규칙을 덮어쓰려는 시도를 여기서 막는다
            if (!"user".equals(message.role()) && !"assistant".equals(message.role())) {
                continue;
            }
            messages.addObject()
                    .put("role", message.role())
                    .put("content", message.content());
        }

        return MAPPER.writeValueAsString(root);
    }

    /**
     * 정상 응답에서 답변 글자를 꺼낸다.
     *
     * 답변은 choices 목록의 첫 번째 -> message -> content 에 들어 있다.
     */
    private String contentOf(String responseBody) {
        JsonNode choice;

        try {
            choice = MAPPER.readTree(responseBody).path("choices").path(0);
        } catch (Exception exc) {
            log.warn("OpenAI 응답이 JSON 이 아닙니다. url={}, 앞부분={}", chatUrl,
                    responseBody.substring(0, Math.min(200, responseBody.length())));
            throw ApiException.serviceUnavailable("챗봇 응답을 읽지 못했습니다. 다시 질문해주세요.");
        }

        String content = choice.path("message").path("content").asString("");

        // max-tokens 에 걸려 답변이 중간에 잘리면 JSON 이 깨져서 해석이 안 된다.
        // 이때는 app.openai.max-tokens 를 늘린다 (JSON 답변은 글보다 길다)
        if ("length".equals(choice.path("finish_reason").asString(""))) {
            log.warn("OpenAI 답변이 max-tokens({}) 에 걸려 잘렸습니다. 값을 늘려보세요.", maxTokens);
            throw ApiException.serviceUnavailable("답변이 너무 길어 만들지 못했습니다. 질문을 짧게 다시 해주세요.");
        }

        if (content.isBlank()) {
            throw ApiException.serviceUnavailable("답변을 만들지 못했습니다. 다시 질문해주세요.");
        }

        return content;
    }

    /**
     * OpenAI 가 거절했을 때의 에러.
     *
     * 화면에는 사용자가 알아들을 문장만 주고, 원인은 로그에 남긴다.
     * 로그에는 error.message 대신 type · code 만 적는다 - message 에는
     * 키 일부가 들어 있는 경우가 있다.
     *
     * 로그에서 보이는 code 로 원인을 안다.
     *   invalid_api_key      키가 틀렸다
     *   insufficient_quota   결제 크레딧이 없다
     *   model_not_found      app.openai.model 이름이 틀렸거나 쓸 수 없는 모델이다
     */
    private ApiException failure(int status, String responseBody) {
        String type = "";
        String code = "";

        try {
            JsonNode error = MAPPER.readTree(responseBody).path("error");
            type = error.path("type").asString("");
            code = error.path("code").asString("");
        } catch (Exception exc) {
            // 본문이 JSON 이 아니어도 상태 코드만으로 처리한다
        }

        log.warn("OpenAI 가 요청을 거절했습니다 (HTTP {}, type={}, code={}).", status, type, code);

        if (status == 401) {
            return ApiException.serviceUnavailable("챗봇 설정에 문제가 있습니다. 관리자에게 문의해주세요.");
        }

        // 429 는 두 가지다. 잠깐 몰린 경우, 그리고 크레딧이 바닥난 경우(insufficient_quota).
        // 크레딧 문제는 사용자가 기다려서 풀리지 않으니 일반 장애로 안내한다
        if (status == 429 && !"insufficient_quota".equals(code)) {
            return ApiException.tooManyRequests("요청이 많습니다. 잠시 후 다시 시도해주세요.");
        }

        return ApiException.serviceUnavailable("챗봇이 지금 답변할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}