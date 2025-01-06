package com.dotori.backend.common.exception;

import lombok.Getter;

@Getter
public class AuthException extends BusinessException {
    private final ErrorCode errorCode;

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }
}
