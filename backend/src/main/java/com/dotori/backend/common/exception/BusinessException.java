package com.dotori.backend.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getCode() + " : " + errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 예외가 발생했을 때 StackTrace를 채우는 역할을 하는 메서드
    // StackTrace -> 예외 생성 비용 증가
    // (Stack Depth 10 -> 4000ns 소요 -> 1~5ms
    // 오버라이딩 -> 80ns 정도로 성능 향상
    @Override
    public synchronized Throwable fillInStackTrace() {
        // StackTrace를 채우지 않도록 처리
        return this;
    }

}
