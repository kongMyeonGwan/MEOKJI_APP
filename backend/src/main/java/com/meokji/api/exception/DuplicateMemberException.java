package com.meokji.api.exception;

import org.springframework.http.HttpStatus;

import com.meokji.api.common.ApiException;

import lombok.Getter;

/** 409 아이디 · 이메일 중복. 응답은 ApiExceptionHandler 가 만든다 */
@Getter
public class DuplicateMemberException extends ApiException {

    private final String field; // 겹친 칸 (아이디, 이메일)

    public DuplicateMemberException(String field, String message) {
        super(HttpStatus.CONFLICT, message);
        this.field = field;
    }
}
