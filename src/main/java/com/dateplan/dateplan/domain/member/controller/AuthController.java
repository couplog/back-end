package com.dateplan.dateplan.domain.member.controller;

import static com.dateplan.dateplan.global.constant.Auth.BEARER;

import com.dateplan.dateplan.domain.member.dto.AuthToken;
import com.dateplan.dateplan.domain.member.dto.LoginRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneAuthCodeRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneRequest;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

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

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<Void>> login(@RequestBody @Valid LoginRequest loginRequest) {
		AuthToken authToken = authService.login(loginRequest.toServiceRequest());
		HttpHeaders responseHeaders = setHeaderTokens(authToken);
		return ResponseEntity.ok()
			.headers(responseHeaders)
			.body(ApiResponse.ofSuccess());
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<Void>> refresh(
		@RequestHeader(value = "Authorization") String refreshToken) {
		AuthToken authToken = authService.refreshAccessToken(
			refreshToken.replaceAll(BEARER.getContent(), ""));
		HttpHeaders responseHeaders = setHeaderTokens(authToken);
		return ResponseEntity.ok()
			.headers(responseHeaders)
			.body(ApiResponse.ofSuccess());
	}

	private static HttpHeaders setHeaderTokens(AuthToken authToken) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Authorization", authToken.getAccessToken());
		responseHeaders.set("refreshToken", authToken.getRefreshToken());
		return responseHeaders;
	}
}
