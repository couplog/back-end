package com.dateplan.dateplan.domain.member.controller;

import com.dateplan.dateplan.domain.member.dto.ConnectionResponse;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

	private final MemberService memberService;

	@GetMapping("/connect")
	public ApiResponse<ConnectionResponse> getConnectionCode() {
		ConnectionServiceResponse connectionServiceResponse = memberService.getConnectionCode();
		return ApiResponse.ofSuccess(connectionServiceResponse.toConnectionResponse());
	}

}