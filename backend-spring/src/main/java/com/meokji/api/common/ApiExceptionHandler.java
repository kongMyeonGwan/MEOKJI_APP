package com.meokji.api.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * 모든 오류를 {@code {"detail": ...}} 한 모양으로 내려보낸다.
 *
 * <p>프론트(frontend/src/utils/apiError.js)가 읽는 방식이 딱 두 가지다.
 *
 * <pre>
 *   detail 이 문자열이면   그대로 사용자에게 보여준다
 *   detail 이 배열이면     입력칸별 오류로 나눈다. 각 항목의 loc 마지막 값을 칸
 *                          이름으로, msg 를 그 칸의 문구로 쓴다
 * </pre>
 *
 * <p>배열 쪽은 파이썬(Pydantic)의 422 모양이다. Pydantic 은 {@code type} ·
 * {@code input} · {@code ctx} 도 같이 보내지만 <b>프론트는 읽지 않는다.</b>
 * 그래서 여기서는 {@code loc} 과 {@code msg} 만 만든다 - 안 쓰는 칸을 흉내내면
 * 맞는지 확인할 수 없는 코드만 늘어난다.
 *
 * <h2>왜 {@code ResponseEntityExceptionHandler} 를 상속하나</h2>
 *
 * <p>스프링이 스스로 내는 오류는 한 계통이 아니다. 없는 주소는
 * {@code NoResourceFoundException}(ResponseStatusException 계열)이고, 허용되지 않은
 * 메서드는 {@code HttpRequestMethodNotSupportedException}(ServletException 계열)이다.
 * 처음에는 {@code ResponseStatusException} 하나만 잡았는데 <b>405 가 그물을 빠져나가
 * 500 으로 나갔다.</b>
 *
 * <p>이 부모 클래스가 그 종류를 전부 알고 있고 상태 코드도 제대로 붙여준다.
 * 우리는 {@link #handleExceptionInternal} 한 곳에서 본문 모양만 바꾼다.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** 우리가 직접 낸 오류. 상태 코드와 문구를 그대로 쓴다. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException exc) {
        return ResponseEntity.status(exc.getStatus()).body(detail(exc.getDetail()));
    }

    /**
     * {@code @Valid} 가 걸린 요청 본문이 규칙에 안 맞을 때.
     *
     * <p>부모는 400 을 내지만 422 로 바꾼다. 파이썬(FastAPI)이 422 를 쓰고 있어서,
     * 두 서버의 응답을 기계적으로 비교하려면 코드도 같아야 한다.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exc,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<Map<String, Object>> items = new ArrayList<>();

        for (FieldError error : exc.getBindingResult().getFieldErrors()) {
            // loc 는 ["body", "nickname"] 모양이어야 한다. 프론트가 마지막 값을
            // 칸 이름으로 쓰기 때문에 앞에 "body" 를 붙여줘야 짝이 맞는다.
            items.add(item(List.of("body", error.getField()), error.getDefaultMessage()));
        }

        // 필드 하나가 아니라 객체 전체에 걸린 규칙(@AssertTrue 등).
        for (ObjectError error : exc.getBindingResult().getGlobalErrors()) {
            items.add(item(List.of("body"), error.getDefaultMessage()));
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(detail(items));
    }

    /**
     * 쿼리 파라미터 · 경로 변수에 걸린 규칙(@Min, @Max 등)이 깨졌을 때.
     *
     * <p>{@code Visitor} 를 구현하지 않고 결과 목록을 그냥 훑는다. Visitor 는
     * 스프링이 칸 종류를 늘릴 때마다 메서드가 추가돼서, 올려 쓸 때 컴파일이 깨진다.
     * 우리는 칸 이름과 문구만 필요하다.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exc,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<Map<String, Object>> items = new ArrayList<>();

        for (ParameterValidationResult result : exc.getParameterValidationResults()) {
            String field = result.getMethodParameter().getParameterName();

            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                // 경로 변수와 쿼리 파라미터를 가리지 않는다. 프론트는 loc 의
                // 마지막 값(칸 이름)만 읽기 때문에 앞자리는 무엇이든 상관없다.
                items.add(item(List.of("query", field == null ? "?" : field),
                        error.getDefaultMessage()));
            }
        }

        if (items.isEmpty()) {
            items.add(item(List.of("query"), "요청 값이 올바르지 않습니다."));
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(detail(items));
    }

    /**
     * 부모가 처리한 오류의 본문을 우리 모양으로 바꾼다.
     *
     * <p>없는 주소(404) · 허용되지 않은 메서드(405) · 읽을 수 없는 본문(400) 같은
     * 것이 전부 여기를 지난다. 부모가 넣어주는 {@code ProblemDetail} 은 프론트가
     * 읽을 칸이 없으므로 버리고 {@code detail} 하나만 남긴다.
     *
     * <p>404 · 405 문구는 파이썬이 내는 것과 같게 영문으로 둔다. 한국어로 바꾸지
     * 않는 이유는, 이건 사용자에게 보여줄 문구가 아니라 개발자가 주소나 메서드를
     * 틀렸을 때만 보는 값이기 때문이다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exc,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = switch (status) {
            case NOT_FOUND -> "Not Found";
            case METHOD_NOT_ALLOWED -> "Method Not Allowed";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported Media Type";
            default -> status.getReasonPhrase();
        };

        return ResponseEntity.status(status).headers(headers).body(detail(message));
    }

    /** 서비스 계층에서 직접 돌린 검증기가 깨졌을 때. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException exc) {
        List<Map<String, Object>> items = new ArrayList<>();

        for (ConstraintViolation<?> violation : exc.getConstraintViolations()) {
            items.add(item(List.of("body", lastNode(violation)), violation.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(detail(items));
    }

    /**
     * 위에서 안 걸린 것 전부.
     *
     * <p>원인 문구를 화면에 내보내지 않는다. 커넥션 문자열이나 SQL 이 섞여 나갈 수
     * 있어서다(파이썬 health 라우터가 예외 타입만 남기는 것과 같은 이유). 대신
     * 서버 로그에는 스택을 그대로 남긴다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exc) {
        logger.error("처리하지 못한 예외", exc);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(detail("요청을 처리하지 못했습니다."));
    }

    // ------------------------------------------------------------------ 도우미

    private static Map<String, Object> detail(Object value) {
        // LinkedHashMap 을 쓰는 이유는 칸 순서를 고정하려는 것이다.
        // 정답지(src/test/resources/fixtures)와 글자 단위로 비교할 때 편하다.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("detail", value);
        return body;
    }

    private static Map<String, Object> item(List<String> loc, String msg) {
        Map<String, Object> one = new LinkedHashMap<>();
        one.put("loc", loc);
        one.put("msg", msg == null ? "값이 올바르지 않습니다." : msg);
        return one;
    }

    /** "register.request.nickname" 에서 "nickname" 만 꺼낸다. */
    private static String lastNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }
}
