package com.meokji.api.exception;

import org.springframework.http.HttpStatus;

import com.meokji.api.common.ApiException;

/** 401 로그인 필요. 응답은 ApiExceptionHandler 가 만든다 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
