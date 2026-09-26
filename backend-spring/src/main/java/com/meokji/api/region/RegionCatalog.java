package com.meokji.api.region;

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
 * <p>파이썬 쪽 {@code app/data/regions.py} 자리다. 데이터 파일
 * ({@code resources/data/regions.json})은 파이썬 쪽에서 <b>그대로 복사</b>했다.
 * 손으로 옮기면 좌표 소수점이 어긋나고, 그러면 날씨가 다른 동네 것으로 나온다.
 *
 * <p>표를 만들지 않고 파일에 두는 이유는 파이썬 쪽과 같다 - 사용자가 만드는 값이
 * 아니고 목록이 바뀌지 않는다.
 */
@Component
public class RegionCatalog {

    private static final String PATH = "data/regions.json";

    /** 시·도 이름 -> (시·군·구 이름 -> 좌표). 순서를 유지해야 화면 드롭다운이 흔들리지 않는다. */
    private final Map<String, Sido> byName = new LinkedHashMap<>();

    /** 시·군·구를 안 골랐을 때 떨어뜨릴 시·도 중심 좌표까지 같이 들고 있다. */
    private record Sido(BigDecimal lat, BigDecimal lon, Map<String, BigDecimal[]> sigungu) {
    }

    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.builder().build();

        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = mapper.readTree(in);

            for (JsonNode sido : root) {
                Map<String, BigDecimal[]> children = new LinkedHashMap<>();

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
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", exc);
        }
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        // BigDecimal 로 읽는다. double 로 받으면 37.5177 이 37.51769999... 로 떠서
        // DB 에 넣는 값과 응답 값이 파이썬과 달라진다.
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    /** 화면에 내려줄 목록. {@code [{name, sigungu: [이름...]}, ...]} */
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();

        byName.forEach((name, sido) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("sigungu", new ArrayList<>(sido.sigungu().keySet()));
            result.add(row);
        });

        return result;
    }

    /**
     * 주소를 좌표로 바꾼다. 찾지 못하면 null.
     *
     * <p>시·군·구를 안 골랐거나 목록에 없으면 <b>시·도 중심으로 떨어뜨린다.</b>
     * 좌표가 아예 없는 것보다 시청 근처 날씨라도 보여주는 편이 낫다
     * (파이썬 {@code find_coords} 와 같은 판단).
     */
    public BigDecimal[] findCoords(String sido, String sigungu) {
        if (sido == null || sido.isBlank()) {
            return null;
        }

        Sido found = byName.get(sido);
        if (found == null) {
            return null;
        }

        if (sigungu != null && !sigungu.isBlank()) {
            BigDecimal[] coords = found.sigungu().get(sigungu);
            if (coords != null) {
                return coords;
            }
        }

        if (found.lat() == null || found.lon() == null) {
            return null;
        }

        return new BigDecimal[] { found.lat(), found.lon() };
    }
}
