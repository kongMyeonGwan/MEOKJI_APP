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

    private Price price = new Price();

    private Otp otp = new Otp();

    private Mail mail = new Mail();

    /**
     * 이메일 인증번호 (STEP 8 · 33).
     *
     * <p>파이썬 쪽 {@code OTP_*} 설정과 같은 값이어야 한다. 두 서버가 같은
     * {@code verification_codes} 표를 보기 때문에, 만료 시간이 서로 다르면 한쪽에서
     * 받은 코드가 다른 쪽에서 조용히 만료된다.
     */
    public static class Otp {

        /** 발급한 코드가 살아 있는 시간(분). 메일 본문에도 이 값이 적힌다. */
        private int expireMinutes = 5;

        /** 한 코드에 몇 번까지 틀려볼 수 있는지. 넘으면 429 로 끊고 재발급을 시킨다. */
        private int maxAttempt = 5;

        /** 재발송 제한(초). 1분에 한 번만 보낸다 (STEP 33). */
        private int resendCooldownSeconds = 60;

        public int getExpireMinutes() {
            return expireMinutes;
        }

        public void setExpireMinutes(int expireMinutes) {
            this.expireMinutes = expireMinutes;
        }

        public int getMaxAttempt() {
            return maxAttempt;
        }

        public void setMaxAttempt(int maxAttempt) {
            this.maxAttempt = maxAttempt;
        }

        public int getResendCooldownSeconds() {
            return resendCooldownSeconds;
        }

        public void setResendCooldownSeconds(int resendCooldownSeconds) {
            this.resendCooldownSeconds = resendCooldownSeconds;
        }
    }

    /**
     * 메일 발송.
     *
     * <p>접속 정보(호스트 · 계정 · 비밀번호)는 여기 두지 않고 스프링 표준인
     * {@code spring.mail.*} 을 쓴다. 우리가 정하는 것은 "보낼지 말지"와 발신자뿐이다.
     */
    public static class Mail {

        /**
         * mock : 콘솔에 인증번호를 찍는다. 메일 서버 없이 화면을 끝까지 볼 수 있다
         * smtp : 진짜로 보낸다. {@code spring.mail.host} 등이 채워져 있어야 한다
         */
        private String provider = "mock";

        /** 보내는 사람. 비워두면 {@code spring.mail.username} 을 쓴다. */
        private String from = "";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    /** 재료 시세 연동 (KAMIS). */
    public static class Price {

        /**
         * 쉼표로 여러 개를 받게 열어뒀지만 <b>지금 쓸 수 있는 값은 kamis 하나뿐이다.</b>
         * 모르는 값을 적으면 조용히 건너뛰지 않고 503 을 낸다 - 오타를 못 찾는 것보다
         * 낫다.
         */
        private String providers = "kamis";

        /** 공공데이터포털 일반 인증키의 Decoding 값. */
        private String serviceKey = "";

        /**
         * 서버가 뜰 때 떨이 재료를 미리 훑을지.
         *
         * <p>기본값이 꺼짐인 것은 테스트 때문이다. 켜두면 {@code gradlew test} 를
         * 돌릴 때마다 바깥 API 를 101번 두드린다. 실제로 띄울 때만 켠다.
         */
        private boolean warmBargains = false;

        /**
         * 훑은 결과를 남겨둘 파일. 실행 위치 기준 상대 경로다.
         *
         * <p>{@code resources/data} 가 아니라 바깥에 둔다 — 그쪽은 파이썬에서
         * 내보낸 <b>고정된 표</b>가 있는 자리이고, 이 파일은 날마다 바뀌는 결과라
         * 성격이 다르다. {@code .gitignore} 에도 넣어뒀다.
         */
        private String bargainFile = "data/bargains.json";

        public String getProviders() {
            return providers;
        }

        public void setProviders(String providers) {
            this.providers = providers;
        }

        public String getServiceKey() {
            return serviceKey;
        }

        public void setServiceKey(String serviceKey) {
            this.serviceKey = serviceKey;
        }

        public boolean isWarmBargains() {
            return warmBargains;
        }

        public void setWarmBargains(boolean warmBargains) {
            this.warmBargains = warmBargains;
        }

        public String getBargainFile() {
            return bargainFile;
        }

        public void setBargainFile(String bargainFile) {
            this.bargainFile = bargainFile;
        }

        /** PRICE_PROVIDERS 를 리스트로. 앞에서부터 시도하다가 값이 나오면 멈춘다. */
        public java.util.List<String> getProviderList() {
            return java.util.Arrays.stream(providers.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .toList();
        }
    }

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

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public Otp getOtp() {
        return otp;
    }

    public void setOtp(Otp otp) {
        this.otp = otp;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }
}
