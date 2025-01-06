package com.dotori.backend.common.handler;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.dotori.backend.domain.member.service.JwtService;
import com.dotori.backend.domain.member.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

	private final JwtService jwtService;
	private final RedisService redisService;

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) {

		Optional<String> accessTokenOpt = jwtService.extractAccessToken(request);

		if (!accessTokenOpt.isEmpty()) {
			Optional<String> emailOpt = jwtService.extractEmailFromAccessToken(request, response, accessTokenOpt.get());
			if (!emailOpt.isEmpty()) {
				String email = emailOpt.get();
				// 로그아웃 전 refreshToken 블랙리스트 처리
				Optional<String> refreshTokenOpt = redisService.getRefreshToken(email);
				if (!refreshTokenOpt.isEmpty()) {
					redisService.addToBlacklist(refreshTokenOpt.get());
					redisService.removeRefreshToken(email);
				}
			}
		}

		// 쿠키에서 accessToken 제거
		jwtService.removeAccessToken(response);
		jwtService.removeRefreshToken(response);

		log.info("[onLogoutSuccess] success");
		// 클라이언트에 로그아웃 완료 응답
		response.setStatus(HttpServletResponse.SC_OK);

	}

}
