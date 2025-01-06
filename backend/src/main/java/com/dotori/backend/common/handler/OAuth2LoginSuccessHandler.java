package com.dotori.backend.common.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotori.backend.common.exception.BusinessException;
import com.dotori.backend.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.dotori.backend.common.config.PathProperty;
import com.dotori.backend.domain.member.model.dto.auth.CustomOAuth2User;
import com.dotori.backend.domain.member.model.entity.Member;
import com.dotori.backend.domain.member.repository.MemberRepository;
import com.dotori.backend.domain.member.service.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
//@Transactional
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
	private final JwtService jwtService;
	private final MemberRepository memberRepository;
	private final PathProperty pathProperty;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		SecurityContextHolder.getContext().setAuthentication(authentication);

		CustomOAuth2User oAuth2User = (CustomOAuth2User)authentication.getPrincipal();

		// access, refresh 토큰 생성
		loginSuccess(response, oAuth2User);

	}

	private void loginSuccess(HttpServletResponse response, CustomOAuth2User oAuth2User) throws IOException {

		String accessToken = jwtService.createAccessToken(oAuth2User.getEmail(), oAuth2User.getRole().name());
		String refreshToken = jwtService.createRefreshToken(oAuth2User.getEmail(), oAuth2User.getRole().name());

		String email = oAuth2User.getEmail();

		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		member.updateRefreshToken(refreshToken);
		memberRepository.save(member);	// Transactional 처리가 되어있지 않으므로 더티 체킹 X -> save 직접 호출

		jwtService.addAccessTokenToCookie(response, accessToken);
		jwtService.addRefreshTokenToCookie(response, refreshToken);

		jwtService.updateRefreshToken(oAuth2User.getEmail(), refreshToken);

		response.sendRedirect(pathProperty.getDOMAIN());
	}
}