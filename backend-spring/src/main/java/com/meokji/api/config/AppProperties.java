package com.meokji.api.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 {@code app.*} 를 담는다.
 *
 * <p>파이썬 쪽 {@code app/core/config.py} 의 {@code Settings} 자리다. 값을 코드에
 * 흩어놓지 않고 한 군데서 읽는 이유도 같다 - 네트워크가 바뀌거나 키가 바뀔 때
 * 고칠 곳이 한 곳이어야 한다.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 화면과 /api/health 에 나가는 서비스 이름. */
    private String name = "뭐먹지? API";

    private String version = "0.1.0";

    /**
     * local 이 아니면 막는 동작이 있다(데모 데이터 스크립트 등).
     * DB 를 보고 판단하지 않고 이 값으로 판단한다 - "비어 있으면 로컬이겠지" 같은
     * 추측은 한 번 틀리면 돌이킬 수 없다.
     */
    private String env = "local";

    /** 쉼표로 여러 개. React(Vite) 개발 서버가 5173 이다. */
    private String corsOrigins = "http://localhost:5173";

    private int maxLoginFailCount = 20;

    private Jwt jwt = new Jwt();

    private Weather weather = new Weather();

    /** 날씨 연동. mock 이면 바깥을 부르지 않는다. */
    public static class Weather {

        private String provider = "mock";
        private String apiKey = "";

        /** 좌표를 안 보냈고 회원 주소도 없을 때 쓸 자리. */
        private double defaultLat = 37.5711;
        private double defaultLon = 126.9885;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public double getDefaultLat() {
            return defaultLat;
        }

        public void setDefaultLat(double defaultLat) {
            this.defaultLat = defaultLat;
        }

        public double getDefaultLon() {
            return defaultLon;
        }

        public void setDefaultLon(double defaultLon) {
            this.defaultLon = defaultLon;
        }
    }

    public static class Jwt {

        /**
         * HS256 서명 키. 32바이트(영문 32자) 이상이어야 한다 -
         * 짧으면 서버가 뜨다가 WeakKeyException 으로 죽는다.
         */
        private String secretKey;

        /** 분 단위. 응답의 expires_in 이 이 값 * 60 으로 나간다. */
        private int expireMinutes = 30;

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public int getExpireMinutes() {
            return expireMinutes;
        }

        public void setExpireMinutes(int expireMinutes) {
            this.expireMinutes = expireMinutes;
        }

        /** 토큰 응답의 expires_in 에 그대로 들어가는 값(초). */
        public long getExpiresInSeconds() {
            return expireMinutes * 60L;
        }
    }

    /** CORS_ORIGINS 를 리스트로. 빈 칸은 버린다. */
    public List<String> getCorsOriginList() {
        return Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getCorsOrigins() {
        return corsOrigins;
    }

    public void setCorsOrigins(String corsOrigins) {
        this.corsOrigins = corsOrigins;
    }

    public int getMaxLoginFailCount() {
        return maxLoginFailCount;
    }

    public void setMaxLoginFailCount(int maxLoginFailCount) {
        this.maxLoginFailCount = maxLoginFailCount;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }
}
