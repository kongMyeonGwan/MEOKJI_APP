package com.meokji.api.util;

import java.io.InputStream;
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
 * 읍·면·동의 실제 경계. 좌표가 어느 동 안에 들어가는지 알려준다.
 *
 * 중심점끼리 거리를 재는 방법(RegionCatalog.findNearest)은 동 단위에서 자주 틀린다.
 * 종로1·2·3·4가동처럼 길쭉한 동은 중심점이 동네를 대표하지 못해서, 종각역에 서 있어도
 * 1km 떨어진 중구 명동이 더 가깝게 나왔다. 경계 안에 들어가는지 보면 그런 일이 없다.
 *
 * 경계 출처는 resources/data/regions-출처.txt 에 적어두었다.
 */
@Component
public class RegionBoundary {

    private static final String PATH = "data/dong-boundaries.json";

    /** 동 하나의 경계. 테두리는 [위도, 경도, 위도, 경도, ...] 로 늘어놓는다. */
    private static class Area {

        private String sido;

        private String sigungu;

        private String dong;

        // 테두리를 다 훑기 전에 "애초에 이 근처가 아니다" 를 걸러내는 네모
        private double minLat;

        private double minLon;

        private double maxLat;

        private double maxLon;

        private double[][] rings;
    }

    private final List<Area> areas = new ArrayList<>();

    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.builder().build();

        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = mapper.readTree(in);

            for (JsonNode node : root) {
                Area area = new Area();
                area.sido = node.path("sido").asString();
                area.sigungu = node.path("sigungu").asString();
                area.dong = node.path("dong").asString();

                JsonNode box = node.path("box");
                area.minLat = box.path(0).asDouble();
                area.minLon = box.path(1).asDouble();
                area.maxLat = box.path(2).asDouble();
                area.maxLon = box.path(3).asDouble();

                JsonNode rings = node.path("rings");
                area.rings = new double[rings.size()][];

                for (int index = 0; index < rings.size(); index++) {
                    JsonNode ring = rings.path(index);
                    double[] points = new double[ring.size()];

                    for (int at = 0; at < ring.size(); at++) {
                        points[at] = ring.path(at).asDouble();
                    }

                    area.rings[index] = points;
                }

                areas.add(area);
            }
        } catch (Exception exc) {
            // 이 파일이 없으면 위치로 동을 못 찾는다. 조용히 넘어가면 중심점 방식으로
            // 돌아가서 "가끔 엉뚱한 동이 잡힌다" 가 되므로 시작 자체를 막는다.
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", exc);
        }
    }

    /** 좌표가 들어 있는 동. 바다나 국경 밖이면 null. */
    public Map<String, String> find(double lat, double lon) {
        for (int index = 0; index < areas.size(); index++) {
            Area area = areas.get(index);

            // 네모 밖이면 테두리를 볼 것도 없다. 3,555개 중 거의 다 여기서 걸러진다.
            if (lat < area.minLat || lat > area.maxLat
                    || lon < area.minLon || lon > area.maxLon) {
                continue;
            }

            if (contains(area, lat, lon)) {
                Map<String, String> result = new LinkedHashMap<>();
                result.put("sido", area.sido);
                result.put("sigungu", area.sigungu);
                result.put("dong", area.dong);
                return result;
            }
        }

        return null;
    }

    /**
     * 점이 테두리 안에 있는지.
     *
     * 점에서 동쪽으로 반직선을 하나 긋고, 테두리와 몇 번 만나는지 센다. 홀수면 안,
     * 짝수면 밖이다. 섬이 여러 개인 동(테두리가 여러 개)도 다 세면 그대로 맞는다.
     */
    private static boolean contains(Area area, double lat, double lon) {
        int crossings = 0;

        for (double[] ring : area.rings) {
            int count = ring.length / 2;

            for (int index = 0; index < count; index++) {
                int next = (index + 1) % count;

                double fromLat = ring[index * 2];
                double fromLon = ring[index * 2 + 1];
                double toLat = ring[next * 2];
                double toLon = ring[next * 2 + 1];

                // 변이 내 위도를 가로지를 때만 본다
                if ((fromLat > lat) != (toLat > lat)) {
                    // 그 위도에서 변이 지나는 경도
                    double cross = (toLon - fromLon) * (lat - fromLat) / (toLat - fromLat) + fromLon;

                    if (lon < cross) {
                        crossings++;
                    }
                }
            }
        }

        return crossings % 2 == 1;
    }
}
