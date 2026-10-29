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
 * 시·도 / 시·군·구 / 읍·면·동 목록과 그 중심 좌표.
 *
 * 읍·면·동 좌표의 출처는 resources/data/regions-출처.txt 에 적어두었다
 * (통계청 SGIS 행정동 경계를 가공한 자료, CC BY 4.0).
 */
@Component
public class RegionCatalog {

    private static final String PATH = "data/regions.json";

    /** 시·도 이름 -> 그 아래 내용. 순서를 유지해야 화면 드롭다운이 흔들리지 않는다. */
    private final Map<String, Sido> byName = new LinkedHashMap<>();

    /** 시·군·구를 안 골랐을 때 떨어뜨릴 시·도 중심 좌표까지 같이 들고 있다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Sido {

        private BigDecimal lat;

        private BigDecimal lon;

        /** 시·군·구 이름 -> 그 아래 내용 */
        private Map<String, Sigungu> sigungu;
    }

    /** 읍·면·동을 안 골랐을 때 떨어뜨릴 시·군·구 중심 좌표까지 같이 들고 있다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Sigungu {

        private BigDecimal lat;

        private BigDecimal lon;

        /** 읍·면·동 이름 -> 좌표 */
        private Map<String, BigDecimal[]> dong;
    }

    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.builder().build();

        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = mapper.readTree(in);

            for (JsonNode sido : root) {
                Map<String, Sigungu> children = new LinkedHashMap<>();

                for (JsonNode child : sido.path("sigungu")) {
                    Map<String, BigDecimal[]> dongs = new LinkedHashMap<>();

                    for (JsonNode dong : child.path("dong")) {
                        dongs.put(dong.path("name").asString(), new BigDecimal[] {
                                decimal(dong, "lat"), decimal(dong, "lon"),
                        });
                    }

                    children.put(child.path("name").asString(),
                            new Sigungu(decimal(child, "lat"), decimal(child, "lon"), dongs));
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

    /**
     * 화면에 내려줄 목록. [{name, sigungu: [이름...]}, ...]
     *
     * 읍·면·동은 빼고 준다. 전국 3,500 개를 같이 실으면 응답이 400KB 를 넘는데,
     * 그 중 화면이 실제로 쓰는 건 고른 시·군·구 하나뿐이다. 동은 시·군·구를 고른
     * 뒤에 listDong 으로 따로 받아간다.
     */
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

    /** 고른 시·군·구에 딸린 읍·면·동 이름 목록. 없으면 빈 목록. */
    public List<String> listDong(String sido, String sigungu) {
        Sigungu found = findSigungu(sido, sigungu);

        if (found == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(found.getDong().keySet());
    }

    /**
     * 좌표에서 가장 가까운 읍·면·동을 찾는다. 찾지 못하면 null.
     *
     * 바깥 API 를 부르지 않고 regions.json 에 있는 중심 좌표들과 거리를 재서 고른다.
     * 위도 1도와 경도 1도는 길이가 다르지만(우리나라에서 경도 쪽이 약 0.8배 짧다)
     * 가장 가까운 하나만 고르면 되는 자리라 그대로 비교해도 결과가 같다.
     *
     * 세종특별자치시처럼 읍·면·동이 시·군·구 자리에 들어간 곳은 dong 이 빈 글자로 온다.
     */
    public Map<String, String> findNearest(double lat, double lon) {
        String nearestSido = null;
        String nearestSigungu = null;
        String nearestDong = "";
        double nearestGap = Double.MAX_VALUE;

        for (Map.Entry<String, Sido> sido : byName.entrySet()) {
            for (Map.Entry<String, Sigungu> sigungu : sido.getValue().getSigungu().entrySet()) {
                Sigungu child = sigungu.getValue();

                // 동이 있으면 동끼리 견준다. 동이 더 촘촘해서 시·군·구 중심보다 늘 가깝다.
                for (Map.Entry<String, BigDecimal[]> dong : child.getDong().entrySet()) {
                    double gap = gapTo(dong.getValue(), lat, lon);

                    if (gap < nearestGap) {
                        nearestGap = gap;
                        nearestSido = sido.getKey();
                        nearestSigungu = sigungu.getKey();
                        nearestDong = dong.getKey();
                    }
                }

                // 동이 없는 시·군·구는 그 중심 좌표로 견준다.
                if (child.getDong().isEmpty()) {
                    double gap = gapTo(new BigDecimal[] { child.getLat(), child.getLon() }, lat, lon);

                    if (gap < nearestGap) {
                        nearestGap = gap;
                        nearestSido = sido.getKey();
                        nearestSigungu = sigungu.getKey();
                        nearestDong = "";
                    }
                }
            }
        }

        if (nearestSido == null) {
            return null;
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("sido", nearestSido);
        result.put("sigungu", nearestSigungu);
        result.put("dong", nearestDong);

        return result;
    }

    /** 좌표가 얼마나 먼지. 제곱근을 씌우지 않는다. 어느 쪽이 가까운지만 보면 되기 때문이다. */
    private static double gapTo(BigDecimal[] coords, double lat, double lon) {
        if (coords == null || coords[0] == null || coords[1] == null) {
            return Double.MAX_VALUE;
        }

        double latGap = coords[0].doubleValue() - lat;
        double lonGap = coords[1].doubleValue() - lon;

        return latGap * latGap + lonGap * lonGap;
    }

    /**
     * 주소를 좌표로 바꾼다. 찾지 못하면 null.
     *
     * 자세한 쪽을 먼저 본다. 읍·면·동 -> 시·군·구 -> 시·도 순으로 내려가면서
     * 찾은 첫 좌표를 쓴다.
     */
    public BigDecimal[] findCoords(String sido, String sigungu, String dong) {
        if (sido == null || sido.isBlank()) {
            return null;
        }

        Sido found = byName.get(sido);
        if (found == null) {
            return null;
        }

        Sigungu child = findSigungu(sido, sigungu);

        if (child != null) {
            if (dong != null && !dong.isBlank()) {
                BigDecimal[] coords = child.getDong().get(dong);
                if (coords != null) {
                    return coords;
                }
            }

            if (child.getLat() != null && child.getLon() != null) {
                return new BigDecimal[] { child.getLat(), child.getLon() };
            }
        }

        if (found.getLat() == null || found.getLon() == null) {
            return null;
        }

        return new BigDecimal[] { found.getLat(), found.getLon() };
    }

    private Sigungu findSigungu(String sido, String sigungu) {
        if (sido == null || sido.isBlank() || sigungu == null || sigungu.isBlank()) {
            return null;
        }

        Sido found = byName.get(sido);
        if (found == null) {
            return null;
        }

        return found.getSigungu().get(sigungu);
    }
}
