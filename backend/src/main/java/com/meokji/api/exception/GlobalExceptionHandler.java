package com.meokji.api.exception;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // @param message: 전체 안내문그 
    // @param errors : 칸별 안내문구
    private Map<String, Object> body(String message, Map<String, String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("errors", errors);
        return body;
    }

    // 400 Bad Requesst: 입력값 규칙(@Valid) 위반
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {

        // 안내문구를 담을 상자
        Map<String, String> errors=new LinkedHashMap<>();

        //규칙을 어긴 칸들을 하나씩 꺼내서 담음
        for(FieldError fieldError: e.getBindingResult().getFieldErrors()){
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(body("입력값을 다시 확인해 주세요.", errors));
    }

    // 로그인 실패. 로그인 필요
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException e) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", e.getMessage());                     // 예) "아이디 또는 비밀번호가 올바르지 않습니다."
        body.put("errors", new LinkedHashMap<String, String>()); // 특정 입력칸 문제가 아니므로 비워 둔다

        // 401 = "누구인지 확인되지 않았다"
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // 409 Conflient: 아이디 / 이메일 중복 되었을 떄
    @ExceptionHandler(DuplicateMemberException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateMember(DuplicateMemberException e) {

        // 안내문구를 담을 상자
        Map<String, String> errors = new LinkedHashMap<>();

        // 규칙을 어긴 칸들을 하나씩 꺼내서 담음
        errors.put(e.getField(), e.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", e.getMessage());
        body.put("errors", errors);

        // 409 상태코드 + JSON 본문으로 응답
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // 500 Internal Server Error : 예상하지 못한 DB 무결성 오류
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException e) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("message", "데이터 처리 중 오류가 발생했습니다.");
        body.put("errors", new LinkedHashMap<String, String>());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /** [403] 권한 없음 (남의 글 수정·삭제) ― [Chapter 04 추가] */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body(e.getMessage(), new LinkedHashMap<>()));
    }

    /** [404] 대상 없음 (없는 글 번호) ― [Chapter 04 추가] */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(e.getMessage(), new LinkedHashMap<>()));
    }
}
