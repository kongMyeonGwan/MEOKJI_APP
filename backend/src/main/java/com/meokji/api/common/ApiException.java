package com.meokji.api.common;

import org.springframework.http.HttpStatus;

/**
 * 화면에 보여줄 에러.
 *
 * 서비스에서 이렇게 던지면
 *     throw ApiException.notFound("등록되지 않은 재료입니다.");
 * ApiExceptionHandler 가 받아서 404 + {"detail": "등록되지 않은 재료입니다."} 로 응답한다.
 */
public class ApiException extends RuntimeException {

    private HttpStatus status; // 응답 상태 코드 (404, 409 ...)
    private Integer retryAfterSeconds; // 429 일 때만. 몇 초 뒤에 다시 되는지

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(HttpStatus status, String message, Integer retryAfterSeconds) {
        super(message);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    // ----- 자주 쓰는 에러를 짧게 만드는 메서드들 -----

    // 400 잘못된 요청
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    // 401 로그인이 필요함 (토큰이 없거나 만료됨)
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    // 403 로그인은 했지만 권한이 없음 (예: 관리자 전용)
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    // 404 찾을 수 없음
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    // 409 이미 있음 (예: 이미 사용 중인 이메일)
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    // 423 계정이 잠김
    public static ApiException locked(String message) {
        return new ApiException(HttpStatus.LOCKED, message);
    }

    // 429 너무 자주 요청함
    public static ApiException tooManyRequests(String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    // 429 + 몇 초 뒤에 다시 되는지
    public static ApiException tooManyRequests(String message, int retryAfterSeconds) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message, retryAfterSeconds);
    }

    // 422 값이 규칙에 안 맞음
    public static ApiException unprocessable(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    // 503 바깥 서비스(날씨, 시세 API)가 응답하지 않음
    public static ApiException serviceUnavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    // 501 아직 만들지 않은 기능 (게시판, 공지)
    public static ApiException notImplemented(String message) {
        return new ApiException(HttpStatus.NOT_IMPLEMENTED, message);
    }
}
