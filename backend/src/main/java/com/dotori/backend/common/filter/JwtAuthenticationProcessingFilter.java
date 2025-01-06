package com.dotori.backend.common.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotori.backend.common.exception.BusinessException;
import com.dotori.backend.common.exception.ErrorCode;
import com.dotori.backend.domain.member.service.RedisService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dotori.backend.domain.member.model.entity.Member;
import com.dotori.backend.domain.member.repository.MemberRepository;
import com.dotori.backend.domain.member.service.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Jwt 인증 필터
 * "/login" 이외의 URI 요청이 왔을 때 처리하는 필터
 *
 * 기본적으로 사용자는 요청 헤더에 AccessToken만 담아서 요청
 * AccessToken 만료 시에만 refreshToken을 요청 (헤더에 AccessToken과 함께 요청)
 *
 * 1. refreshToken이 없고, AccessToken이 유효한 경우 -> 인증 성공 처리, refreshToken을 재발급하지는 않는다.
 * 2. refreshToken이 없고, AccessToken이 없거나 유효하지 않은 경우 -> 인증 실패 처리, 403 ERROR
 * 3. refreshToken이 있는 경우 -> DB의 refreshToken과 비교하여 일치하면 AccessToken 재발급, refreshToken 재발급(RTR 방식)
 * 인증 성공 처리는 하지 않고 실패 처리
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final MemberRepository memberRepository;
	private static final String NO_CHECK_URL = "/reaccesstoken";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		if (request.getRequestURI().contains(NO_CHECK_URL)) {
			filterChain.doFilter(request, response);
			return;		// 필터 진행 막기
		}

		Optional<String> accessTokenOpt = jwtService.extractAccessToken(request);

		if (accessTokenOpt.isPresent()) {
			String accessToken = accessTokenOpt.get();

			String email = jwtService.extractEmailFromAccessToken(request, response, accessToken)
					.orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

			String role = jwtService.extractRoleFromAccessToken(request, response, accessToken)
					.orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

			Member member = memberRepository.findByEmail(email)
					.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

			SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
			List<GrantedAuthority> authorities = Collections.singletonList(authority);

			// Authentication 객체 생성
			Authentication authentication = new UsernamePasswordAuthenticationToken(member, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			filterChain.doFilter(request, response);

		} else {
			filterChain.doFilter(request, response);
		}

	}

}