package com.dotori.backend.domain.member.service;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.dotori.backend.common.exception.BusinessException;
import com.dotori.backend.common.exception.ErrorCode;
import com.dotori.backend.common.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.dotori.backend.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JwtService {

	@Value("${jwt.secretKey}")
	private String secretKey;

	@Value("${jwt.access.expiration}")
	private Long accessTokenExpirationPeriod;

	@Value("${jwt.refresh.expiration}")
	private Long refreshTokenExpirationPeriod;

	@Value("${jwt.access.header}")
	private String accessHeader;

	@Value("${jwt.refresh.header}")
	private String refreshHeader;

	private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
	private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
	private static final String EMAIL_CLAIM = "email";
	private static final String ROLE_CLAIM = "role";

	private final MemberRepository memberRepository;
	private final RedisService redisService;

	/**
	 * AccessToken 생성
	 */
	public String createAccessToken(String email, String role) {
		Date now = new Date();
		return JWT.create()
			.withSubject(ACCESS_TOKEN_SUBJECT)
			.withExpiresAt(new Date(now.getTime() + accessTokenExpirationPeriod))
			.withClaim(EMAIL_CLAIM, email)
			.withClaim(ROLE_CLAIM, "ROLE_" + role)
			.sign(Algorithm.HMAC512(secretKey));
	}

	/**
	 * RefreshToken 생성
	 */
	public String createRefreshToken(String email, String role) {
		Date now = new Date();
		return JWT.create()
			.withSubject(REFRESH_TOKEN_SUBJECT)
			.withExpiresAt(new Date(now.getTime() + refreshTokenExpirationPeriod))
			.withClaim(EMAIL_CLAIM, email)
			.withClaim(ROLE_CLAIM, "ROLE_" + role)
			.sign(Algorithm.HMAC512(secretKey));
	}

	/**
	 * AccessToken 응답
	 */
	public void addAccessTokenToCookie(HttpServletResponse response, String accessToken) {
		ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
			.httpOnly(true) // JavaScript 접근 방지
			// .secure(true) // https 에서만 전송
			.path("/") // 쿠키 경로
			.sameSite("Lax") // SameSite 설정
			.build();

		response.addHeader("Set-Cookie", cookie.toString());
		log.info("[sendAccessToken] success");
	}

	/**
	 * RefreshToken 응답
	 */
	public void addRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true) // JavaScript 접근 방지
			// .secure(true) // https 에서만 전송
			.path("/api/members/reaccesstoken")
			.sameSite("Lax")
			.build();

		response.addHeader("Set-Cookie", cookie.toString());
		log.info("[sendRefreshToken] success");
	}

	/**
	 * 쿠키에서 AccessToken 추출
	 */
	public Optional<String> extractAccessToken(HttpServletRequest request) {
		if (request.getCookies() != null) {
			return Arrays.stream(request.getCookies())
					.filter(cookie -> "accessToken".equals(cookie.getName()))
					.findFirst()
					.map(Cookie::getValue);
		}

		return Optional.empty();
	}

	public Optional<String> extractRefreshToken(HttpServletRequest request) {
		if (request.getCookies() != null) {
			return Arrays.stream(request.getCookies())
					.filter(cookie -> "refreshToken".equals(cookie.getName()))
					.findFirst()
					.map(Cookie::getValue);
		}

		return Optional.empty();
	}

	private Optional<String> extractEmail(String accessToken) throws JWTVerificationException {

		// accessToken 유효성 검증
		String email = JWT.require(Algorithm.HMAC512(secretKey))
				.build()
				.verify(accessToken)
				.getClaim(EMAIL_CLAIM)
				.asString();

		if (email.isEmpty()) return Optional.empty();
		else return Optional.of(email);

	}

	private Optional<String> extractRole(String accessToken) throws JWTVerificationException {

		// accessToken 유효성 검증
		String role = JWT.require(Algorithm.HMAC512(secretKey))
				.build()
				.verify(accessToken)
				.getClaim(ROLE_CLAIM) // claim(Role) 가져오기
				.asString();

		if (role == null || role.isEmpty()) return Optional.empty();
		else return Optional.of(role);

	}

	public Optional<String> extractEmailFromAccessToken(HttpServletRequest request, HttpServletResponse response, String token) {
		try {
			isTokenValid(token);
		} catch (TokenExpiredException e) {
			reIssueAccessToken(request, response);
		} catch (JWTVerificationException e) {
			// accessToken을 재발급하지 않을 것 (유효하지 않은 accessToken)
			throw new AuthException(ErrorCode.ACCESS_TOKEN_INVALID);
		}

		Optional<String> emailOpt = extractEmail(token);

		if (emailOpt.isPresent()) return emailOpt;
		else return Optional.empty();
	}

	private void reIssueAccessToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshToken(request)
				.orElseThrow(() -> new AuthException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

		String email = extractEmailFromRefreshToken(refreshToken)
				.orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

		String role = extractRoleFromRefreshToken(refreshToken)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

		String newAccessToken = createAccessToken(email, role);
		// accessToken을 재발급 할 것 (accessToken 만료)
		addAccessTokenToCookie(response, newAccessToken);

	}

	public Optional<String> extractRoleFromAccessToken(HttpServletRequest request, HttpServletResponse response, String token) {
		try {
			isTokenValid(token);
		} catch (TokenExpiredException e) {
			reIssueAccessToken(request, response);
		} catch (JWTVerificationException e) {
			throw new AuthException(ErrorCode.ACCESS_TOKEN_INVALID);
		}

		Optional<String> roleOpt = extractRole(token);

		if (roleOpt.isPresent()) return roleOpt;
		else return Optional.empty();
	}

	public Optional<String> extractEmailFromRefreshToken(String token) {
		try {
			isTokenValid(token);
		} catch (TokenExpiredException e) {
			throw new AuthException(ErrorCode.REFRESH_TOKEN_EXPIRED);	// 재로그인
		} catch (JWTVerificationException e) {
			throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);	// 재로그인
		}

		Optional<String> emailOpt = extractEmail(token);

		if (emailOpt.isPresent()) return emailOpt;
		else return Optional.empty();
	}

	private Optional<String> extractRoleFromRefreshToken(String token) {
		try {
			isTokenValid(token);
		} catch (TokenExpiredException e) {
			throw new AuthException(ErrorCode.REFRESH_TOKEN_EXPIRED);	// 재로그인
		} catch (JWTVerificationException e) {
			throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);	// 재로그인
		}

		Optional<String> roleOpt = extractRole(token);

		if (roleOpt.isPresent()) return roleOpt;
		else return Optional.empty();
	}

	public void updateRefreshToken(String email, String refreshToken) {
		redisService.saveRefreshToken(email, refreshToken, refreshTokenExpirationPeriod, TimeUnit.MILLISECONDS);
	}

	private void isTokenValid(String token) throws JWTVerificationException {
		JWT.require(Algorithm.HMAC512(secretKey)).build().verify(token);
	}

	public void removeAccessToken(HttpServletResponse response) {
		Cookie cookie = new Cookie("accessToken", null);
		cookie.setMaxAge(0);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		response.addCookie(cookie);
		log.info("[removeAccessToken] success");
	}

	public void removeRefreshToken(HttpServletResponse response) {
		Cookie cookie = new Cookie("refreshToken", null);
		cookie.setMaxAge(0);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		response.addCookie(cookie);
		log.info("[removeRefreshToken] success");
	}
}