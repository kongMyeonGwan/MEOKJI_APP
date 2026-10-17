package com.meokji.api.exception;

import org.springframework.http.HttpStatus;

import com.meokji.api.common.ApiException;

/** 403 권한 불충분 (남의 글 수정 · 삭제 등). 응답은 ApiExceptionHandler 가 만든다 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
