package com.dateplan.dateplan.domain.member.controller;

import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.domain.member.dto.ConnectionRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionResponse;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

	private final CoupleService coupleService;

	@GetMapping("/connect")
	public ApiResponse<ConnectionResponse> getConnectionCode() {
		ConnectionServiceResponse connectionServiceResponse = coupleService.getConnectionCode();
		return ApiResponse.ofSuccess(connectionServiceResponse.toConnectionResponse());
	}

	@PostMapping("/connect")
	public ApiResponse<Void> connectCouple(@Valid @RequestBody ConnectionRequest request) {
		coupleService.connectCouple(request.toConnectionServiceRequest());
		return ApiResponse.ofSuccess();
	}
}
