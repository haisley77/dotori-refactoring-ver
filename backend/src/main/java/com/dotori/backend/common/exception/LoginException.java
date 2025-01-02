package com.dotori.backend.common.exception;

import lombok.Getter;

@Getter
public class LoginException extends BusinessException {
    private final ErrorCode errorCode;

    public LoginException(ErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }
}
