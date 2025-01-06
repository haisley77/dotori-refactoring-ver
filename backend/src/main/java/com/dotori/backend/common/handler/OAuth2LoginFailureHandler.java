package com.dotori.backend.common.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		log.info("social authentication failure", exception.getMessage());
	}
}