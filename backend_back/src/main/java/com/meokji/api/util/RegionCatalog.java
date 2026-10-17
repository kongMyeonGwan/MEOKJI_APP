package com.meokji.api.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 시·도 / 시·군·구 목록과 그 중심 좌표.
 *
 * <p>쓰는 곳이 둘이다
 *   list()       : 가입 · 정보수정 화면의 주소 드롭다운 (이름만 내려준다)
 *   findCoords() : 고른 주소를 좌표로 바꿔 USERS 에 저장 (날씨를 부를 때 쓴다)
 *
 * <p>왜 표가 아니라 파일인가 — 행정구역은 사용자가 만드는 값이 아니고 몇 년에
 * 한 번 바뀐다. 표로 두면 INSERT 문 250줄을 관리해야 하고, 좌표를 고칠 때
 * 운영 DB 를 건드려야 한다. resources/data/regions.json 에 두고 서버가 뜰 때
 * 한 번 읽는다.
 *
 * <p>좌표를 같이 들고 있는 이유 — 날씨 API 는 주소가 아니라 위도 · 경도를 받는다.
 * 사용자는 "서울특별시 강남구" 를 고르는데 API 는 좌표를 요구하므로, 그 사이를
 * 바꿔줄 표가 필요하다. 바깥 지오코딩 API 를 쓰면 가입이 그 API 사정에
 * 끌려가므로 직접 들고 있다.
 */
@Component
public class RegionCatalog {

    private static final String PATH = "data/regions.json";

    /** 시·도 이름 -> (시·군·구 이름 -> 좌표). 순서를 유지해야 화면 드롭다운이 흔들리지 않는다. */
    private final Map<String, Sido> byName = new LinkedHashMap<>();

    /**
     * 시·군·구를 안 골랐을 때 떨어뜨릴 시·도 중심 좌표까지 같이 들고 있다.
     *
     * 시·도 좌표가 왜 따로 필요한가 — 주소를 시·도까지만 고르고 저장하는 사람이
     * 있다. 그때 좌표가 없으면 날씨를 못 부른다. 시·군·구 중 하나를 임의로
     * 골라 쓰면 "서울" 인데 날씨가 강남구 기준이 되므로, 시·도 중심 좌표를
     * 따로 적어 두었다.
     *
     * private static class 로 안쪽에 둔 이유 — 이 클래스 밖에서는 쓸 일이
     * 없는 모양이다. vo 폴더에 꺼내 놓으면 "화면에 나가는 값" 으로 보인다.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Sido {

        /** 시·도 중심 위도 */
        private BigDecimal lat;

        /** 시·도 중심 경도 */
        private BigDecimal lon;

        /** 시·군·구 이름 -> [위도, 경도]. 길이 2 인 배열이다 */
        private Map<String, BigDecimal[]> sigungu;
    }

    /**
     * 서버가 뜰 때 한 번 파일을 읽어 메모리에 올린다.
     *
     * {@code @PostConstruct} 는 "이 빈이 만들어진 직후에 불러라" 는 표시다.
     * 요청이 올 때마다 25KB 를 다시 읽고 파싱할 이유가 없다. 내용이 바뀌지
     * 않으므로 한 번 읽어 두면 그대로 쓸 수 있다.
     */
    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.builder().build();

        // try-with-resources - 블록을 벗어나면 스트림이 알아서 닫힌다.
        // 서버 시작에 한 번뿐이라 안 닫아도 티는 안 나지만, 열고 안 닫는
        // 모양을 남겨두면 다른 곳에서 그대로 베껴 쓴다.
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = mapper.readTree(in);

            // regions.json 은 시·도 배열이다. 한 칸이 시·도 하나.
            for (JsonNode sido : root) {
                Map<String, BigDecimal[]> children = new LinkedHashMap<>();

                // 그 아래 시·군·구를 이름 -> 좌표 로 바꿔 담는다.
                // 배열로 두면 findCoords 가 쓸 때마다 이름으로 훑어야 한다.
                for (JsonNode child : sido.path("sigungu")) {
                    children.put(child.path("name").asString(), new BigDecimal[] {
                            decimal(child, "lat"), decimal(child, "lon"),
                    });
                }

                byName.put(sido.path("name").asString(),
                        new Sido(decimal(sido, "lat"), decimal(sido, "lon"), children));
            }
        } catch (Exception exc) {
            // 이 파일이 없으면 가입에서 좌표를 못 넣고 날씨도 못 부른다.
            // 조용히 빈 목록으로 뜨면 원인을 찾기 어려우므로 시작 자체를 막는다.
            //
            // 서버가 떠 있는데 주소 드롭다운만 비어 있으면 "프론트 문제인가" 부터
            // 의심하게 된다. 아예 안 뜨면 로그 맨 위에 이 문장이 보인다.
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", exc);
        }
    }

    /**
     * JSON 의 숫자 한 칸을 BigDecimal 로 꺼낸다. 칸이 없거나 null 이면 null.
     *
     * 값이 없는 지역이 있을 수 있어서 null 을 정상으로 본다. findCoords 가
     * 그때 null 을 돌려주고, 부르는 쪽이 좌표 없이 처리한다.
     */
    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        // BigDecimal 로 읽는다. double 로 받으면 37.5177 이 37.51769999... 로 떠서
        // 날씨 API 에 보낼 때 자릿수가 지저분해지고, 로그로 봐도 파일에 적은
        // 값과 달라 보인다. 좌표는 계산할 값이 아니라 그대로 전달할 값이다.
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    /**
     * 화면에 내려줄 목록. [{name, sigungu: [이름...]}, ...]
     *
     * 좌표를 빼고 이름만 보내는 이유 — 드롭다운은 이름만 보여준다. 좌표는
     * 가입할 때 서버가 findCoords 로 직접 찾으므로 화면이 알 필요가 없고,
     * 같이 보내면 응답 크기가 두 배가 된다.
     *
     * Sido 객체를 그대로 내보내지 않는 이유도 같다. Sido 에는 좌표가 들어 있다.
     */
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();

        byName.forEach((name, sido) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            // keySet 을 그대로 넘기면 Map 과 이어진 뷰가 나간다.
            // 새 ArrayList 로 복사해 이름 배열만 보낸다.
            row.put("sigungu", new ArrayList<>(sido.getSigungu().keySet()));
            result.add(row);
        });

        return result;
    }

    /**
     * 주소를 좌표로 바꾼다. 찾지 못하면 null.
     *
     * 찾는 순서가 있다
     *   1. 시·군·구까지 맞으면 그 좌표
     *   2. 시·군·구가 없거나 목록에 없으면 시·도 중심 좌표
     *   3. 시·도도 못 찾으면 null
     *
     * 2번이 있는 이유 — 시·군·구 이름은 바뀌기도 하고, 사용자가 옛 이름으로
     * 저장해 둔 경우도 있다. 그때 null 을 주면 날씨가 통째로 안 나오는데,
     * 시·도 중심이라도 쓰면 "대략 맞는" 날씨는 보여줄 수 있다.
     *
     * 에러를 던지지 않고 null 을 돌려주는 이유 — 좌표를 못 찾는 것은 가입을
     * 막을 일이 아니다. 좌표 없이 가입하고, 날씨는 기본 지역으로 보여준다.
     *
     * @return [위도, 경도] 길이 2 인 배열. 못 찾으면 null
     */
    public BigDecimal[] findCoords(String sido, String sigungu) {
        // isBlank 는 공백만 있는 문자열("  ")도 걸러낸다.
        // 화면에서 빈 칸으로 보낸 값이 null 이 아니라 "" 로 올 수 있다.
        if (sido == null || sido.isBlank()) {
            return null;
        }

        Sido found = byName.get(sido);
        if (found == null) {
            return null;
        }

        // 1. 시·군·구까지 맞는지 먼저 본다
        if (sigungu != null && !sigungu.isBlank()) {
            BigDecimal[] coords = found.getSigungu().get(sigungu);
            if (coords != null) {
                return coords;
            }
            // 못 찾으면 여기서 끝내지 않고 아래 시·도 좌표로 떨어진다
        }

        // 2. 시·도 중심 좌표. 파일에 좌표가 비어 있으면 null
        if (found.getLat() == null || found.getLon() == null) {
            return null;
        }

        return new BigDecimal[] { found.getLat(), found.getLon() };
    }
}
