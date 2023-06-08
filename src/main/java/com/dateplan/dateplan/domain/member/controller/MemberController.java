package com.dateplan.dateplan.domain.member.controller;

import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.domain.member.dto.ConnectionRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionResponse;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.dto.PresignedURLResponse;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

	private final CoupleService coupleService;
	private final MemberService memberService;

	@GetMapping("/{member_id}/profile/image/presigned-url")
	public ApiResponse<PresignedURLResponse> getPresignedURL(@PathVariable("member_id") Long memberId) {

		PresignedURLResponse presingedURL = memberService.getPresignedURLForProfileImage(memberId);

		return ApiResponse.ofSuccess(presingedURL);
	}

	@PutMapping("/{member_id}/profile/image")
	public ApiResponse<Void> modifyProfileImage(@PathVariable("member_id") Long memberId) {

		memberService.checkAndSaveProfileImage(memberId);

		return ApiResponse.ofSuccess();
	}

	@DeleteMapping("/{member_id}/profile/image")
	public ApiResponse<Void> deleteProfileImage(@PathVariable("member_id") Long memberId) {

		memberService.deleteProfileImage(memberId);

		return ApiResponse.ofSuccess();
	}

	@GetMapping("/{member_id}/connect")
	public ApiResponse<ConnectionResponse> getConnectionCode(
		@PathVariable("member_id") Long memberId) {
		ConnectionServiceResponse connectionServiceResponse = coupleService.getConnectionCode(memberId);
		return ApiResponse.ofSuccess(connectionServiceResponse.toConnectionResponse());
	}

	@PostMapping("/{member_id}/connect")
	public ApiResponse<Void> connectCouple(@PathVariable("member_id") Long memberId,
		@Valid @RequestBody ConnectionRequest request) {
		coupleService.connectCouple(memberId, request.toConnectionServiceRequest());
		return ApiResponse.ofSuccess();
	}
}
