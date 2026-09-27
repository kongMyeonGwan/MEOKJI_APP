package com.meokji.api.common;

import org.springframework.http.HttpStatus;

/**
 * 화면에 그대로 보여줄 오류.
 *
 * <p>파이썬 쪽 {@code HTTPException(status, detail)} 자리다. 프론트는 응답 본문의
 * {@code detail} 한 칸만 읽어서 사용자에게 보여준다(frontend/src/utils/apiError.js).
 * 그래서 서버가 내는 모든 오류는 이 예외를 거쳐 {@code {"detail": "..."}} 모양으로
 * 나가야 한다.
 *
 * <p>Spring 기본 오류 본문은 {@code {"timestamp", "status", "error", "path"}} 라서
 * 프론트가 읽을 칸이 없다. {@link ApiExceptionHandler} 가 그것까지 바꿔준다.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    /**
     * 429 에 붙일 {@code Retry-After} 값(초). 그 외에는 null 이다.
     *
     * <p>문구에도 남은 초가 적히지만 화면이 문구를 파싱하게 두면 안 된다. 표준
     * 헤더로 같이 내보내서 화면이 숫자만 읽고 셀 수 있게 한다.
     */
    private final Integer retryAfterSeconds;

    public ApiException(HttpStatus status, String detail) {
        this(status, detail, null);
    }

    public ApiException(HttpStatus status, String detail, Integer retryAfterSeconds) {
        super(detail);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDetail() {
        return getMessage();
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    // 자주 쓰는 것만 짧게 만들어 둔다. 라우터에서 읽기 쉽게 하려는 것이다.

    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, detail);
    }

    /** 토큰이 없거나 만료됐거나 위조된 경우. */
    public static ApiException unauthorized(String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, detail);
    }

    /** 로그인은 했지만 권한이 없는 경우. 404 로 숨기지 않고 403 을 낸다. */
    public static ApiException forbidden(String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, detail);
    }

    /** 이미 있는 이메일 · 닉네임처럼 지금 상태와 부딪히는 경우. */
    public static ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, detail);
    }

    /** 잠긴 계정. */
    public static ApiException locked(String detail) {
        return new ApiException(HttpStatus.LOCKED, detail);
    }

    /** 너무 잦은 요청. 인증번호 재발송 제한과 인증 시도 횟수 초과가 쓴다. */
    public static ApiException tooManyRequests(String detail) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, detail);
    }

    /** 위와 같되 {@code Retry-After} 헤더까지 붙인다. */
    public static ApiException tooManyRequests(String detail, int retryAfterSeconds) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, detail, retryAfterSeconds);
    }

    /**
     * 몸통은 맞지만 값이 규칙에 안 맞는 경우.
     *
     * <p>422 다. Spring 기본값은 400 이지만 파이썬(FastAPI)이 422 를 쓰고 있어서
     * 맞춘다 - 프론트가 상태 코드로 "입력칸별 오류" 를 가려낸다.
     */
    public static ApiException unprocessable(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, detail);
    }

    /** 외부 API 설정이 잘못됐거나 응답이 없는 경우. */
    public static ApiException serviceUnavailable(String detail) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, detail);
    }

    /**
     * 아직 만들지 않은 기능.
     *
     * <p>게시판(posts)과 공지(notices)가 이걸 쓴다. 404 면 "주소가 틀렸나" 가 되고
     * 500 이면 "내가 뭘 잘못했나" 가 된다. 501 은 "안 만들었다" 는 뜻이라 /docs 를
     * 열어본 담당자에게 그대로 읽힌다.
     */
    public static ApiException notImplemented(String detail) {
        return new ApiException(HttpStatus.NOT_IMPLEMENTED, detail);
    }
}
