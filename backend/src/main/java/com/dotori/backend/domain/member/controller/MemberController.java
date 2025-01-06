package com.dotori.backend.domain.member.controller;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotori.backend.common.exception.BusinessException;
import com.dotori.backend.common.exception.ErrorCode;
import com.dotori.backend.common.exception.AuthException;
import com.dotori.backend.domain.member.model.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dotori.backend.domain.member.model.entity.Member;
import com.dotori.backend.domain.member.repository.MemberRepository;
import com.dotori.backend.domain.member.service.JwtService;
import com.dotori.backend.domain.member.service.MemberService;
import com.dotori.backend.domain.member.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private final JwtService jwtService;
	private final MemberRepository memberRepository;
	private final RedisService redisService;
	private final MemberService memberService;

	@GetMapping("/status")
	public ResponseEntity<?> getAuthStatus() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		boolean isAnonymous = authentication instanceof AnonymousAuthenticationToken;
		boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !isAnonymous;

		return ResponseEntity.ok(Collections.singletonMap("isAuthenticated", isAuthenticated));
	}

	@GetMapping("/detail")
	public ResponseEntity<?> getMemberInfo(HttpServletRequest request) {

		String accessToken = jwtService.extractAccessToken(request)
				.orElseThrow(() -> new AuthException(ErrorCode.ACCESS_TOKEN_NOT_FOUND));

		String email = jwtService.extractEmailFromAccessToken(accessToken)
				.orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		MemberResponseDto result = new MemberResponseDto(member);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/{memberId}/videos")
	public ResponseEntity<GetMemberVideosResponse> getMemberVideos(
			@PathVariable(name = "memberId") Long memberId) {

		List<MemberVideoDto> memberVideos = memberService.getMemberVideos(memberId);

		GetMemberVideosResponse result = new GetMemberVideosResponse(memberVideos);
		return ResponseEntity.ok().body(result);
	}

	@GetMapping("/reaccesstoken")
	public ResponseEntity<?> reAccessToken(HttpServletRequest request, HttpServletResponse response) {

		String refreshToken = jwtService.extractRefreshToken(request)
				.orElseThrow(() -> new AuthException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

		String email = jwtService.extractEmailFromRefreshToken(refreshToken)
				.orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

		String redisRefreshToken = redisService.getRefreshToken(email)
				.orElseThrow(() -> new AuthException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));	// 재로그인

		if (redisService.isBlacklisted(redisRefreshToken)) {
			throw new AuthException(ErrorCode.REFRESH_TOKEN_EXPIRED);		// 재로그인
		}

		if (!refreshToken.equals(redisRefreshToken)) {
			throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);		// 재로그인
		}

		String accessToken = jwtService.createAccessToken(email, "USER");
		jwtService.sendAccessToken(response, accessToken);

		return ResponseEntity.ok().build();

	}

	@PutMapping("/update-nickname")
	public ResponseEntity<?> updateNickname(HttpServletRequest request,
		@RequestParam("newNickname") String newNickname) {

		String accessToken = jwtService.extractAccessToken(request)
				.orElseThrow(() -> new AuthException(ErrorCode.ACCESS_TOKEN_NOT_FOUND));

		String email = jwtService.extractEmailFromAccessToken(accessToken)
				.orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

		memberService.updateNickname(email, newNickname);

		return ResponseEntity.ok().build();
	}

	@PutMapping(value = "/profile-image")
	public ResponseEntity<ProfileImageUpdateResponse> updateProfileImg(HttpServletRequest request,
		@Validated ProfileImageUpdateRequest profileImageUpdateRequest) {

		String accessToken = jwtService.extractAccessToken(request)
				.orElseThrow(() -> new AuthException(ErrorCode.ACCESS_TOKEN_NOT_FOUND));

		String email = jwtService.extractEmailFromAccessToken(accessToken)
				.orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

		String savedPath = memberService.updateProfileImage(email, profileImageUpdateRequest);

		ProfileImageUpdateResponse result = new ProfileImageUpdateResponse(savedPath);
		return ResponseEntity.ok(result);
	}

}
