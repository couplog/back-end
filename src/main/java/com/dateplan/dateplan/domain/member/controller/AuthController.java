package com.dateplan.dateplan.domain.member.controller;

import static com.dateplan.dateplan.global.constant.Auth.BEARER;

import com.dateplan.dateplan.domain.member.service.dto.response.AuthToken;
import com.dateplan.dateplan.domain.member.controller.dto.request.LoginRequest;
import com.dateplan.dateplan.domain.member.controller.dto.response.LoginResponse;
import com.dateplan.dateplan.domain.member.service.dto.response.LoginServiceResponse;
import com.dateplan.dateplan.domain.member.dto.signup.PhoneAuthCodeRequest;
import com.dateplan.dateplan.domain.member.dto.signup.PhoneRequest;
import com.dateplan.dateplan.domain.member.dto.signup.SendSmsResponse;
import com.dateplan.dateplan.domain.member.dto.signup.SendSmsServiceResponse;
import com.dateplan.dateplan.domain.member.dto.signup.SignUpRequest;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
	public ApiResponse<SendSmsResponse> sendCode(@RequestBody @Valid PhoneRequest request) {

		SendSmsServiceResponse serviceResponse = authService.sendSms(
			request.toServiceRequest());

		return ApiResponse.ofSuccess(serviceResponse.toResponse());
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
	public ResponseEntity<ApiResponse<LoginResponse>> login(
		@RequestBody @Valid LoginRequest loginRequest) {
		LoginServiceResponse response = authService.login(loginRequest.toServiceRequest());
		HttpHeaders responseHeaders = setHeaderTokens(response.getAuthToken());
		return ResponseEntity.ok()
			.headers(responseHeaders)
			.body(ApiResponse.ofSuccess(LoginResponse.from(response)));
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

	private HttpHeaders setHeaderTokens(AuthToken authToken) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Authorization", authToken.getAccessToken());
		responseHeaders.set("refreshToken", authToken.getRefreshToken());
		return responseHeaders;
	}
}
