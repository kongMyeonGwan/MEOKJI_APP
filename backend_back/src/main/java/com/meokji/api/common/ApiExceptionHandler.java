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
 *
 * <p>왜 한 군데 모았나
 *
 * <p>@RestControllerAdvice 는 "모든 컨트롤러 밖에서 에러를 기다린다" 는 뜻이다.
 * 이게 없으면 컨트롤러 메서드마다 try-catch 를 두르고 ResponseEntity 를 만들어야
 * 한다. 메서드가 50개면 같은 코드가 50벌이 되고, 한 곳에서 응답 모양을
 * {"message": ...} 로 적는 실수가 섞인다.
 *
 * <p>응답 모양을 detail 한 칸으로 통일한 이유 — 프론트의 axios 인터셉터가
 * 에러를 받으면 detail 만 꺼내 화면에 띄운다. 칸 이름이 에러마다 다르면
 * 프론트가 "이 에러는 어느 칸을 봐야 하지" 를 가려야 한다.
 *
 * <p>detail 이 문장일 때와 배열일 때 — 2 · 3 번(검사 실패)만 배열이다. 어느 칸이
 * 왜 틀렸는지를 칸별로 알려줘야 화면이 그 입력칸 아래에 빨간 글씨를 붙일 수
 * 있기 때문이다. 나머지는 띄울 문장 하나면 된다.
 *
 * <p>메서드 순서는 좁은 것에서 넓은 것으로 간다. 스프링은 가장 잘 맞는 핸들러를
 * 고르므로 순서 자체가 동작을 바꾸지는 않지만, 5번 Exception 이 "나머지 전부" 라는
 * 것을 읽는 사람이 알 수 있게 아래에 두었다.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    // 1. 우리가 직접 던진 에러
    //    상태 코드와 문장을 ApiException 이 이미 들고 있으므로 그대로 꺼내 쓴다
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException e) {
        // 429(너무 자주 요청) 이면 몇 초 뒤에 다시 되는지 헤더로 알려준다
        //
        // 문장에 "60초 뒤에 다시 시도해주세요" 라고만 적으면 화면이 그 숫자를
        // 글자에서 뽑아내야 한다. 헤더로 따로 주면 버튼에 남은 초를 세는 데
        // 그대로 쓸 수 있다. (WebConfig 의 exposedHeaders 에 열어둔 헤더다)
        if (e.getRetryAfterSeconds() != null) {
            return ResponseEntity.status(e.getStatus())
                    .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                    .body(detail(e.getMessage()));
        }

        return ResponseEntity.status(e.getStatus()).body(detail(e.getMessage()));
    }

    // 2. @Valid 검사 실패 → 422
    //    detail 이 배열이다: [{ "loc": ["body", "nickname"], "msg": "닉네임을 입력해주세요" }, ...]
    //
    //    컨트롤러의 @Valid 가 VO 의 @NotBlank · @Size 를 검사하다 걸리면 스프링이
    //    이 예외를 던진다. 서비스까지 들어오지도 않는다.
    //
    //    loc 의 첫 칸이 "body" 인 이유 — 요청 본문(JSON)에서 온 값이라는 뜻이다.
    //    3번은 주소에서 온 값이라 "query" 가 된다. 화면이 이걸 보고 어느
    //    입력칸에 에러를 붙일지 가린다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleNotValid(MethodArgumentNotValidException e) {
        List<Map<String, Object>> items = new ArrayList<>();

        // 칸 하나에 걸린 검사 (@NotBlank, @Size 등)
        // 여러 칸이 한꺼번에 틀릴 수 있어서 전부 모아 보낸다 - 하나씩 알려주면
        // 사용자가 고치고 보내고 또 고치기를 반복한다
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            items.add(item("body", error.getField(), error.getDefaultMessage()));
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(detail(items));
    }

    // 3. 주소 값(@PathVariable, @RequestParam) 검사 실패 → 422
    //    예) 목록 주소에 ?page=0 을 보냈을 때 (@Min(1) 에 걸린다)
    //
    //    2번과 예외 클래스가 다른 이유 — 2번은 객체 하나(VO)를 검사한 결과이고,
    //    이쪽은 메서드 파라미터 여러 개를 각각 검사한 결과다. 그래서 꺼내는
    //    방법도 getFieldErrors 가 아니라 파라미터별로 한 겹 더 들어간다.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleMethodValidation(HandlerMethodValidationException e) {
        List<Map<String, Object>> items = new ArrayList<>();

        // 바깥 for : 파라미터 하나 (page, size ...)
        // 안쪽 for : 그 파라미터에 걸린 검사 (@Min 과 @Max 가 같이 걸릴 수도 있다)
        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            String name = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                items.add(item("query", name, error.getDefaultMessage()));
            }
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(detail(items));
    }

    // 4-1. 없는 주소 → 404
    //      스프링 기본 에러 화면(whitelabel)을 application.yml 에서 껐기 때문에
    //      여기서 받아 JSON 으로 돌려준다. 안 받으면 빈 응답이 나간다.
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
    //      셋을 한 메서드로 묶은 이유는 화면에서 할 일이 똑같기 때문이다.
    //        HttpMessageNotReadable       : JSON 모양이 깨짐
    //        MethodArgumentTypeMismatch   : /api/notices/abc 처럼 숫자 자리에 글자
    //        MissingServletRequestParameter : 꼭 필요한 ?값= 을 안 보냄
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
    //
    //    e.getMessage() 를 그대로 내보내면 표 이름 · 컬럼 이름 · 파일 경로가
    //    바깥으로 새어 나간다. 공격하는 쪽에 알려주는 셈이고, 사용자에게도
    //    읽을 수 없는 영어 문장이라 도움이 안 된다.
    //
    //    대신 log.error 에 e 를 통째로 넘겨 스택트레이스를 남긴다. 고칠 때
    //    볼 곳은 서버 로그다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("처리하지 못한 에러", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail("요청을 처리하지 못했습니다."));
    }

    // {"detail": 값} 모양 만들기
    // 값이 문장일 때도 있고 배열일 때도 있어서 Object 로 받는다
    private Map<String, Object> detail(Object value) {
        Map<String, Object> body = new HashMap<>();
        body.put("detail", value);
        return body;
    }

    // {"loc": [where, field], "msg": message} 모양 만들기
    //   where : "body"(요청 본문) 또는 "query"(주소)
    //   field : 틀린 칸 이름 (nickname, page ...)
    //   msg   : VO 의 @NotBlank(message = "...") 에 적어둔 문장
    private Map<String, Object> item(String where, String field, String message) {
        Map<String, Object> one = new HashMap<>();
        one.put("loc", List.of(where, field));
        one.put("msg", message);
        return one;
    }
}
