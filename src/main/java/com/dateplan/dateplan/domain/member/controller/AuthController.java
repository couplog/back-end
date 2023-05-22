package com.dateplan.dateplan.domain.member.controller;

import com.dateplan.dateplan.domain.member.dto.LoginRequest;
import com.dateplan.dateplan.domain.member.dto.LoginServiceRequest;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ApiResponse<Void> login(
		@RequestBody @Valid LoginRequest loginRequest,
		HttpServletResponse response) {
		LoginServiceRequest loginServiceRequest = LoginServiceRequest.builder()
			.phone(loginRequest.getPhone())
			.password(loginRequest.getPassword())
			.build();
		authService.login(loginServiceRequest, response);
		return ApiResponse.ofSuccess();
	}
}
