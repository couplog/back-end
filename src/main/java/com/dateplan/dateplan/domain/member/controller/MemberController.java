package com.dateplan.dateplan.domain.member.controller;

import com.dateplan.dateplan.domain.member.dto.ConnectionRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionResponse;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.dto.PresignedURLResponse;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.domain.s3.S3ImageType;
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

	private final MemberService memberService;

	@GetMapping("/profile/image/presigned-url")
	public ApiResponse<PresignedURLResponse> getPresignedURL() {

		PresignedURLResponse presingedURL = memberService.getPresingedURL(
			S3ImageType.MEMBER_PROFILE);

		return ApiResponse.ofSuccess(presingedURL);
	}

	@GetMapping("/connect")
	public ApiResponse<ConnectionResponse> getConnectionCode() {
		ConnectionServiceResponse connectionServiceResponse = memberService.getConnectionCode();
		return ApiResponse.ofSuccess(connectionServiceResponse.toConnectionResponse());
	}

	@PostMapping("/connect")
	public ApiResponse<Void> connectCouple(@Valid @RequestBody ConnectionRequest request) {
		memberService.connectCouple(request.toConnectionServiceRequest());
		return ApiResponse.ofSuccess();
	}
}
