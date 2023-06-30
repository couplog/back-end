package com.dateplan.dateplan.domain.member.controller;

import com.dateplan.dateplan.domain.anniversary.service.AnniversaryService;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.domain.member.dto.ConnectionRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionResponse;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.dto.CoupleConnectServiceResponse;
import com.dateplan.dateplan.domain.member.dto.MemberInfoResponse;
import com.dateplan.dateplan.domain.member.dto.MemberInfoServiceResponse;
import com.dateplan.dateplan.domain.member.dto.PresignedURLResponse;
import com.dateplan.dateplan.domain.member.dto.ProfileImageURLResponse;
import com.dateplan.dateplan.domain.member.dto.ProfileImageURLServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
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

	private final MemberService memberService;
	private final MemberReadService memberReadService;
	private final CoupleService coupleService;
	private final CoupleReadService coupleReadService;
	private final AnniversaryService anniversaryService;

	@GetMapping("/me")
	public ApiResponse<MemberInfoResponse> getCurrentLoginMemberInfo() {

		Member loginMember = MemberThreadLocal.get();
		boolean isConnected = coupleReadService.isMemberConnected(loginMember);

		MemberInfoResponse response = MemberInfoResponse.of(loginMember, isConnected);

		return ApiResponse.ofSuccess(response);
	}

	@GetMapping("/partner")
	public ApiResponse<MemberInfoResponse> getPartnerMemberInfo() {

		Long partnerId = coupleReadService.getPartnerId(MemberThreadLocal.get());
		MemberInfoServiceResponse serviceResponse = memberReadService.getMemberInfo(partnerId);

		return ApiResponse.ofSuccess(serviceResponse.toResponse(true));
	}

	@GetMapping("/{member_id}/profile/image")
	public ApiResponse<ProfileImageURLResponse> getProfileImageURL(
		@PathVariable("member_id") Long memberId) {

		Long partnerId = coupleReadService.getPartnerId(MemberThreadLocal.get());

		ProfileImageURLServiceResponse serviceResponse = memberReadService.getProfileImageURL(
			memberId, partnerId);

		return ApiResponse.ofSuccess(serviceResponse.toResponse());
	}

	@GetMapping("/{member_id}/profile/image/presigned-url")
	public ApiResponse<PresignedURLResponse> getPresignedURL(
		@PathVariable("member_id") Long memberId) {

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
		ConnectionServiceResponse response = coupleService.getConnectionCode(memberId);
		return ApiResponse.ofSuccess(ConnectionResponse.from(response));
	}

	@PostMapping("/{member_id}/connect")
	public ApiResponse<Void> connectCouple(@PathVariable("member_id") Long memberId,
		@Valid @RequestBody ConnectionRequest request) {
		CoupleConnectServiceResponse serviceResponse = coupleService.connectCouple(
			memberId, request.toConnectionServiceRequest());
		anniversaryService.createAnniversariesForFirstDate(serviceResponse.getCoupleId());
		anniversaryService.createAnniversariesForBirthDay(serviceResponse.getMember1Id());
		anniversaryService.createAnniversariesForBirthDay(serviceResponse.getMember2Id());
		return ApiResponse.ofSuccess();
	}
}
