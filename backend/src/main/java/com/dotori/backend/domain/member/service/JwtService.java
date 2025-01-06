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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Getter
@Slf4j
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
			.withExpiresAt(new Date(now.getTime() + accessTokenExpirationPeriod)) // 토큰 만료 시간 설정
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
	public void sendAccessToken(HttpServletResponse response, String accessToken) {
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
	public void sendRefreshToken(HttpServletResponse response, String refreshToken) {
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
	public String extractAccessToken(HttpServletRequest request) {
		if (request.getCookies() != null) {
			Optional<String> accessToken = Arrays.stream(request.getCookies())
					.filter(cookie -> "accessToken".equals(cookie.getName()))
					.findFirst()
					.map(Cookie::getValue);

			if (accessToken.isEmpty()) {
				throw new AuthException(ErrorCode.ACCESS_TOKEN_NOT_FOUND);
			}

			return accessToken.get();
		}

		return null;
	}

	public String extractRefreshToken(HttpServletRequest request) {
		if (request.getCookies() != null) {
			Optional<String> refreshToken = Arrays.stream(request.getCookies())
					.filter(cookie -> "refreshToken".equals(cookie.getName()))
					.findFirst()
					.map(Cookie::getValue);

			if (refreshToken.isEmpty()) {
				throw new AuthException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
			}

			return refreshToken.get();
		}

		return null;
	}

	/**
	 * AccessToken에서 Email 추출
	 * 추출 전에 JWT.require()로 검증기 생성
	 * verify로 AceessToken 검증 후
	 * 유효하다면 getClaim()으로 이메일 추출
	 * 유효하지 않다면 빈 Optional 객체 반환
	 */
	public String extractEmail(String accessToken) {
		try {
			// accessToken 유효성 검증
			String email = JWT.require(Algorithm.HMAC512(secretKey))
				.build()
				.verify(accessToken)
				.getClaim(EMAIL_CLAIM)
				.asString();

			if (email == null) {
				throw new BusinessException(ErrorCode.EMAIL_NOT_FOUND);
			}

			return email;

		} catch (TokenExpiredException e) {
			throw new AuthException(ErrorCode.ACCESS_TOKEN_EXPIRED);
		} catch (JWTVerificationException e) {
			throw new AuthException(ErrorCode.ACCESS_TOKEN_INVALID);
		}

	}

	public String extractRole(String accessToken) {
		try {
			// accessToken 유효성 검증
			String role = JWT.require(Algorithm.HMAC512(secretKey))
				.build()
				.verify(accessToken) // accessToken을 검증하고 유효하지 않다면 예외 발생
				.getClaim(ROLE_CLAIM) // claim(Role) 가져오기
				.asString();

			if (role == null || role.isEmpty()) {
				throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
			}

			return role;

		} catch (TokenExpiredException e) {
			throw new AuthException(ErrorCode.ACCESS_TOKEN_EXPIRED);
		} catch (JWTVerificationException e) {
			throw new AuthException(ErrorCode.ACCESS_TOKEN_INVALID);
		}
	}

	public String extractEmailFromAccessToken(HttpServletRequest request) {
		String accessToken = extractAccessToken(request);
		return extractEmail(accessToken);
	}

	public String extractRoleFromAccessToken(HttpServletRequest request) {
		String accessToken = extractAccessToken(request);
		return extractRole(accessToken);
	}

	public String extractEmailFromRefreshToken(HttpServletRequest request) {
		String refreshToken = extractRefreshToken(request);
        return extractEmail(refreshToken);
	}

	public String extractRoleFromRefreshToken(HttpServletRequest request) {
        String refreshToken = extractRefreshToken(request);
		return extractRole(refreshToken);
	}

	/**
	 * refreshToken을 redis에 저장(업데이트)
	 */
	public void updateRefreshToken(String email, String refreshToken) {
		redisService.saveRefreshToken(email, refreshToken, refreshTokenExpirationPeriod, TimeUnit.MILLISECONDS);
	}

	public boolean isTokenValid(String token) {
		try {
			JWT.require(Algorithm.HMAC512(secretKey)).build().verify(token);
			return true;
		} catch (TokenExpiredException e) {
			// 만료된 토큰
			return false;
		} catch (JWTVerificationException e) {
			// 유효하지 않은 토큰
			return false;
		}
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