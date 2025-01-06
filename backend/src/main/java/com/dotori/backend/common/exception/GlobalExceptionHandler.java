package com.dotori.backend.common.exception;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dotori.backend.common.exception.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
	public ResponseEntity<ErrorResponse> methodArgumentNotValidExceptionHandler(
		HttpServletRequest request, BindException bindException
	) {
		printException(bindException);
		List<FieldError> fieldErrors = bindException.getBindingResult()
			.getFieldErrors();

		StringBuilder errorMessage = new StringBuilder();
		errorMessage.append("validation failed fields: [ ");

		int size = fieldErrors.size();
		for (int i = 0; i < size; i++) {
			errorMessage.append(fieldErrors.get(i).getField());
			if (i == size - 1) {
				continue;
			}
			errorMessage.append(", ");
		}
		errorMessage.append(" ]");

		return new ResponseEntity<>(
			ErrorResponse.of(errorMessage.toString(), request.getRequestURI(), fieldErrors),
			HttpStatus.BAD_REQUEST
		);
	}

//	@ExceptionHandler(UsernameNotFoundException.class)
//	public void springSecurityExceptionHandler(UsernameNotFoundException usernameNotFoundException) {
//		// LoginService의 표준 예외를 커스텀 예외로 변경한다.
//		log.info("usernameNotFoundException : " + usernameNotFoundException.getMessage());
//		throw new LoginException(ErrorCode.MEMBER_NOT_FOUND);
//	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ErrorResponse> loginExceptionHandler(
			HttpServletRequest request, AuthException authException
	) {
		// 인증 관련 예외 Logging
		printCustomException(authException);

		// 서버 내부 에러 메시지를 클라이언트에 노출하지 않는다. (커스텀 에러 코드로 처리)
		return new ResponseEntity<>(
				ErrorResponse.of(authException.getErrorCode().toString(), request.getRequestURI()),
				UNAUTHORIZED
		);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> businessExceptionHandler(
			HttpServletRequest request, BusinessException businessException) {
		// 비즈니스 예외 Logging
		printCustomException(businessException);

		// 서버 내부 에러 메시지를 클라이언트에 노출하지 않는다. (커스텀 에러 코드로 처리)
		return new ResponseEntity<>(
			ErrorResponse.of(businessException.getErrorCode().toString(), request.getRequestURI()),
			INTERNAL_SERVER_ERROR
		);

	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponse> runtimeExceptionHandler(
		HttpServletRequest request, RuntimeException runtimeException
	) {
		printException(runtimeException);
		return new ResponseEntity<>(
			ErrorResponse.of(runtimeException.getMessage(), request.getRequestURI()),
			INTERNAL_SERVER_ERROR
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> exceptionHandler(
		HttpServletRequest request, Exception exception
	) {
		printException(exception);
		return new ResponseEntity<>(
			ErrorResponse.of(exception.getMessage(), request.getRequestURI()),
			INTERNAL_SERVER_ERROR
		);
	}

	private void printCustomException(BusinessException exception) {
		log.info("Exception 발생: ", exception.getMessage());
	}

	private void printException(Exception exception) {
		log.error("Exception 발생: ", exception);
	}
}
