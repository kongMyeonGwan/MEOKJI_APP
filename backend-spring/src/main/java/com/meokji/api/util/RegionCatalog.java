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

/** 시·도 / 시·군·구 목록과 그 중심 좌표. */
@Component
public class RegionCatalog {

    private static final String PATH = "data/regions.json";

    /** 시·도 이름 -> (시·군·구 이름 -> 좌표). 순서를 유지해야 화면 드롭다운이 흔들리지 않는다. */
    private final Map<String, Sido> byName = new LinkedHashMap<>();

    /** 시·군·구를 안 골랐을 때 떨어뜨릴 시·도 중심 좌표까지 같이 들고 있다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Sido {

        private BigDecimal lat;

        private BigDecimal lon;

        private Map<String, BigDecimal[]> sigungu;
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
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    /** 화면에 내려줄 목록. [{name, sigungu: [이름...]}, ...] */
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();

        byName.forEach((name, sido) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("sigungu", new ArrayList<>(sido.getSigungu().keySet()));
            result.add(row);
        });

        return result;
    }

    /** 주소를 좌표로 바꾼다. 찾지 못하면 null. */
    public BigDecimal[] findCoords(String sido, String sigungu) {
        if (sido == null || sido.isBlank()) {
            return null;
        }

        Sido found = byName.get(sido);
        if (found == null) {
            return null;
        }

        if (sigungu != null && !sigungu.isBlank()) {
            BigDecimal[] coords = found.getSigungu().get(sigungu);
            if (coords != null) {
                return coords;
            }
        }

        if (found.getLat() == null || found.getLon() == null) {
            return null;
        }

        return new BigDecimal[] { found.getLat(), found.getLon() };
    }
}
