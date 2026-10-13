package com.meokji.api.exception;

public class NotFoundException extends RuntimeException {
    // 404
    public NotFoundException(String message){
        super(message);
    }
}
