package com.meokji.api.exception;

import org.springframework.http.HttpStatus;

import com.meokji.api.common.ApiException;

/**
 * 404 대상 없음 (없는 글 번호 등).
 *
 * ApiException 을 물려받으므로 ApiExceptionHandler 가 그대로 받아
 * {"detail": 문장} 으로 응답한다. 따로 핸들러를 두지 않는다.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
