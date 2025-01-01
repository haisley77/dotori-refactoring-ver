package com.dotori.backend.common.exception;

import com.dotori.backend.common.exception.BusinessException;
import com.dotori.backend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class VideoException extends BusinessException {
    private final ErrorCode errorCode;

    public VideoException(ErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

}
