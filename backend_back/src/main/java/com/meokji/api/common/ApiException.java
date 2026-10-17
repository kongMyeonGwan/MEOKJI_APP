package com.meokji.api.common;

import org.springframework.http.HttpStatus;

/**
 * 화면에 보여줄 에러.
 *
 * 서비스에서 이렇게 던지면
 *     throw ApiException.notFound("등록되지 않은 재료입니다.");
 * ApiExceptionHandler 가 받아서 404 + {"detail": "등록되지 않은 재료입니다."} 로 응답한다.
 *
 * <p>왜 던지고 끝내나 — if 로 분기해서 ResponseEntity 를 만들어 돌려주면,
 * 그 ResponseEntity 를 컨트롤러까지 계층마다 들고 올라와야 한다. 중간에 있는
 * 서비스는 할 일이 없는데도 "아래에서 에러가 왔으면 그대로 올려보내기" 코드를
 * 가져야 한다. 던지면 중간 계층이 아무것도 안 해도 핸들러까지 바로 간다.
 *
 * <p>RuntimeException 을 고른 이유 — Exception 을 상속하면 부르는 쪽마다
 * throws 를 붙이거나 try-catch 로 감싸야 한다. 어차피 잡지 않고 핸들러까지
 * 보낼 에러라서 그 선언이 전부 군더더기가 된다.
 *
 * <p>message 는 사용자가 읽을 문장이다. 그래서 한국어로 적고, SQL 이나 클래스
 * 이름 같은 서버 속 사정은 넣지 않는다. 그런 것은 ApiExceptionHandler 가
 * 로그에만 남긴다.
 *
 * <p>@Transactional 과의 관계 — 서비스에서 이것을 던지면 트랜잭션이 되돌려진다
 * (RuntimeException 이기 때문이다). 그래서 "글은 지웠는데 첨부만 남는" 식으로
 * 반쯤 처리된 상태가 남지 않는다.
 */
public class ApiException extends RuntimeException {

    private HttpStatus status; // 응답 상태 코드 (404, 409 ...)
    private Integer retryAfterSeconds; // 429 일 때만. 몇 초 뒤에 다시 되는지

    // 평소에 쓰는 생성자. 상태 코드와 보여줄 문장만 받는다.
    public ApiException(HttpStatus status, String message) {
        super(message); // 문장은 RuntimeException 이 들고 있다 (getMessage 로 꺼낸다)
        this.status = status;
    }

    // 429 전용. "몇 초 뒤에 다시 되는지" 를 같이 담는다.
    // int 가 아니라 Integer 인 이유는, 값이 없는 상태(null)를 둬야
    // 핸들러가 "Retry-After 헤더를 붙일지" 를 가릴 수 있기 때문이다.
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
    //
    // new ApiException(HttpStatus.NOT_FOUND, "...") 대신 notFound("...") 로 쓴다.
    // 상태 코드를 서비스마다 손으로 고르면 "없는 재료" 에 400 을 주는 곳과
    // 404 를 주는 곳이 섞인다. 뜻마다 코드를 한 번만 정해 두었다.

    // 400 잘못된 요청
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    // 401 로그인이 필요함 (토큰이 없거나 만료됨)
    // 화면은 401 을 받으면 로그인 페이지로 보낸다
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    // 403 로그인은 했지만 권한이 없음 (예: 관리자 전용)
    // 401 과 다르다 — 다시 로그인해도 안 되는 경우라서 화면도 그냥 문장만 띄운다
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    // 404 찾을 수 없음
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    // 409 이미 있음 (예: 이미 사용 중인 이메일)
    // 값이 틀린 게 아니라 "지금 상태와 부딪친다" 는 뜻이라 400 과 나눠 쓴다
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    // 423 계정이 잠김
    // 비밀번호를 여러 번 틀려 잠긴 경우. 비밀번호가 맞아도 못 들어간다
    public static ApiException locked(String message) {
        return new ApiException(HttpStatus.LOCKED, message);
    }

    // 429 너무 자주 요청함
    public static ApiException tooManyRequests(String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    // 429 + 몇 초 뒤에 다시 되는지
    // 인증 메일 재발송 제한에서 쓴다. 화면이 이 숫자로 버튼에 남은 초를 센다
    public static ApiException tooManyRequests(String message, int retryAfterSeconds) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message, retryAfterSeconds);
    }

    // 422 값이 규칙에 안 맞음
    // 모양은 맞는데 내용이 규칙에 어긋나는 경우. @Valid 실패도 같은 코드로 나간다
    public static ApiException unprocessable(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    // 503 바깥 서비스(날씨, 시세 API)가 응답하지 않음
    // 우리 잘못이 아니라 "잠시 뒤에 다시 하면 될 수도 있다" 는 뜻이라 500 과 나눈다
    public static ApiException serviceUnavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    // 501 아직 만들지 않은 기능 (게시판, 공지)
    // 주소는 열어 두고 안을 안 만든 자리. 화면이 "준비 중" 을 띄울 수 있다
    public static ApiException notImplemented(String message) {
        return new ApiException(HttpStatus.NOT_IMPLEMENTED, message);
    }
}
