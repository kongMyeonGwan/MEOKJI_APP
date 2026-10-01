package com.meokji.api.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * 서버에서 에러가 나면 여기로 온다. 에러를 {"detail": "문장"} 모양으로 바꿔서 응답한다.
 * (프론트는 detail 칸만 읽어서 화면에 보여준다)
 *
 * 에러 종류별로 메서드가 하나씩 있다.
 *   1. ApiException                   : 우리가 직접 던진 에러 (throw ApiException.notFound(...))
 *   2. MethodArgumentNotValidException : @Valid 검사 실패 (예: @NotBlank 인데 빈 값) → 422
 *   3. HandlerMethodValidationException: 주소 값 검사 실패 (예: @Min(1) 인데 0) → 422
 *   4. 없는 주소 404, 허용 안 된 메서드 405, 잘못된 요청 400, 지원 안 하는 형식 415
 *   5. Exception                       : 그 밖의 모든 에러 → 500
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    // 1. 우리가 직접 던진 에러
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException e) {
        // 429(너무 자주 요청) 이면 몇 초 뒤에 다시 되는지 헤더로 알려준다
        if (e.getRetryAfterSeconds() != null) {
            return ResponseEntity.status(e.getStatus())
                    .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                    .body(detail(e.getMessage()));
        }

        return ResponseEntity.status(e.getStatus()).body(detail(e.getMessage()));
    }

    // 2. @Valid 검사 실패 → 422
    //    detail 이 배열이다: [{ "loc": ["body", "nickname"], "msg": "닉네임을 입력해주세요" }, ...]
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleNotValid(MethodArgumentNotValidException e) {
        List<Map<String, Object>> items = new ArrayList<>();

        // 칸 하나에 걸린 검사 (@NotBlank, @Size 등)
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            items.add(item("body", error.getField(), error.getDefaultMessage()));
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(detail(items));
    }

    // 3. 주소 값(@PathVariable, @RequestParam) 검사 실패 → 422
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleMethodValidation(HandlerMethodValidationException e) {
        List<Map<String, Object>> items = new ArrayList<>();

        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            String name = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                items.add(item("query", name, error.getDefaultMessage()));
            }
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(detail(items));
    }

    // 4-1. 없는 주소 → 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail("Not Found"));
    }

    // 4-2. GET 만 되는 주소에 POST 를 보낸 경우 등 → 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(detail("Method Not Allowed"));
    }

    // 4-3. 요청 본문(JSON)이 깨졌거나, 숫자 자리에 글자가 오는 등 → 400
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail("Bad Request"));
    }

    // 4-4. JSON 이 아닌 형식으로 보낸 경우 → 415
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(detail("Unsupported Media Type"));
    }

    // 5. 그 밖의 모든 에러 → 500
    //    원인(SQL 등)은 화면에 보여주지 않고 서버 로그에만 남긴다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("처리하지 못한 에러", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail("요청을 처리하지 못했습니다."));
    }

    // {"detail": 값} 모양 만들기
    private Map<String, Object> detail(Object value) {
        Map<String, Object> body = new HashMap<>();
        body.put("detail", value);
        return body;
    }

    // {"loc": [where, field], "msg": message} 모양 만들기
    private Map<String, Object> item(String where, String field, String message) {
        Map<String, Object> one = new HashMap<>();
        one.put("loc", List.of(where, field));
        one.put("msg", message);
        return one;
    }
}
