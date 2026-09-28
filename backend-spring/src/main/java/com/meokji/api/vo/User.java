package com.meokji.api.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * USERS 표 한 행.
 *
 * <p>파이썬 쪽 {@code app/models/user.py} 자리다. 컬럼은 snake_case(FOOD_NAME),
 * 여기 필드는 카멜(foodName)이고 그 사이는 MyBatis 의
 * {@code map-underscore-to-camel-case} 가 잇는다.
 *
 * <p>record 를 쓰지 않는다. MyBatis 가 기본 생성자와 setter 로 값을 채우기 때문이다.
 * (record 로 받으려면 생성자 인자 이름을 맞추는 별도 설정이 필요해서, 컬럼이 늘어날
 * 때마다 손이 더 간다.)
 */
public class User {

    private Long id;
    private String email;

    /**
     * bcrypt 해시. <b>응답 DTO 에 절대 담지 않는다.</b>
     * 파이썬이 쓰던 passlib 와 같은 알고리즘이라 기존 회원의 해시가 그대로 통한다.
     */
    private String passwordHash;

    private String nickname;

    /** 1=관리자 2=일반. 가입은 전부 2 이고, 1 은 DB 에서 직접 올린다. */
    private Integer userLevel;

    private String gender;
    private String addressSido;
    private String addressSigungu;

    /**
     * 위도 · 경도. 날씨 API 를 실제로 부르는 값이다.
     *
     * <p>{@code double} 이 아니라 {@code BigDecimal} 이다. 컬럼이
     * {@code NUMBER(9,6)} 이라 소수 여섯 자리가 그대로 의미를 갖는데, double 로
     * 받으면 37.5177 이 37.51769999... 로 떠서 응답 값이 파이썬과 달라진다.
     */
    private BigDecimal latitude;
    private BigDecimal longitude;

    /** 연속 로그인 실패 횟수. 성공하면 0 으로 되돌린다. */
    private Integer loginFailCount;

    /** 0=정상 1=잠김. 컬럼이 NUMBER(1) 이라 boolean 이 아니다. */
    private Integer isLocked;

    private LocalDateTime lockedAt;

    /** 'N' = 탈퇴 회원. 행은 남기고 조회에서만 뺀다. */
    private String useYn;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 관리자인지. 응답의 is_admin 이 이 값이다. */
    public boolean isAdmin() {
        return userLevel != null && userLevel == 1;
    }

    /** 계정이 잠겼는지. 컬럼이 숫자라서 여기서 한 번만 뒤집어 쓴다. */
    public boolean isLockedAccount() {
        return isLocked != null && isLocked == 1;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(Integer userLevel) {
        this.userLevel = userLevel;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddressSido() {
        return addressSido;
    }

    public void setAddressSido(String addressSido) {
        this.addressSido = addressSido;
    }

    public String getAddressSigungu() {
        return addressSigungu;
    }

    public void setAddressSigungu(String addressSigungu) {
        this.addressSigungu = addressSigungu;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Integer getLoginFailCount() {
        return loginFailCount;
    }

    public void setLoginFailCount(Integer loginFailCount) {
        this.loginFailCount = loginFailCount;
    }

    public Integer getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Integer isLocked) {
        this.isLocked = isLocked;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
