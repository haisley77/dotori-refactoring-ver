package com.dotori.backend.common.exception;

import com.dotori.backend.common.exception.BusinessException;
import lombok.Getter;

@Getter
public class MemberException extends BusinessException {
    private final ErrorCode errorCode;

    public MemberException(ErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }
}
