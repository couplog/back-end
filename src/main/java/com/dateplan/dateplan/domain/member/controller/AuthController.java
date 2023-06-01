package com.dateplan.dateplan.domain.member.controller;

import static com.dateplan.dateplan.global.constant.Auth.BEARER;

import com.dateplan.dateplan.domain.member.dto.LoginRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneAuthCodeRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneRequest;
import com.dateplan.dateplan.domain.member.dto.SignUpRequest;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final MemberService memberService;

	@PostMapping("/phone")
	public ApiResponse<Void> sendCode(@RequestBody @Valid PhoneRequest request) {

		authService.sendSms(request.toServiceRequest());

		return ApiResponse.ofSuccess();
	}

	@PostMapping("/phone/code")
	public ApiResponse<Void> authenticateCode(@RequestBody @Valid PhoneAuthCodeRequest request) {

		authService.authenticateAuthCode(request.toServiceRequest());
		return ApiResponse.ofSuccess();
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<Void> signup(@RequestBody @Valid SignUpRequest request) {

		memberService.signUp(request.toServiceRequest());

		return ApiResponse.ofSuccess();
	}

	@PostMapping("/login")
	public ApiResponse<Void> login(
		@RequestBody @Valid LoginRequest loginRequest,
		HttpServletResponse response) {
		authService.login(loginRequest.toServiceRequest(), response);
		return ApiResponse.ofSuccess();
	}

	@PostMapping("/refresh")
	public ApiResponse<Void> refresh(
		@RequestHeader(value = "Authorization") String refreshToken,
		HttpServletResponse response) {
		authService.refreshAccessToken(refreshToken.replaceAll(BEARER.getContent(), ""), response);
		return ApiResponse.ofSuccess();
	}
}
