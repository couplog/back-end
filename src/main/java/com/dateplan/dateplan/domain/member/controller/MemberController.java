package com.dateplan.dateplan.domain.member.controller;

import com.dateplan.dateplan.domain.anniversary.service.AnniversaryService;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.domain.member.controller.dto.request.CheckPasswordRequest;
import com.dateplan.dateplan.domain.member.controller.dto.request.ConnectionRequest;
import com.dateplan.dateplan.domain.member.controller.dto.response.CheckPasswordResponse;
import com.dateplan.dateplan.domain.member.controller.dto.response.ConnectionResponse;
import com.dateplan.dateplan.domain.member.controller.dto.response.MemberInfoResponse;
import com.dateplan.dateplan.domain.member.controller.dto.response.PresignedURLResponse;
import com.dateplan.dateplan.domain.member.controller.dto.response.ProfileImageURLResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.domain.member.service.dto.request.CheckPasswordServiceResponse;
import com.dateplan.dateplan.domain.member.service.dto.response.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.service.dto.response.CoupleConnectServiceResponse;
import com.dateplan.dateplan.domain.member.service.dto.response.MemberInfoServiceResponse;
import com.dateplan.dateplan.domain.member.service.dto.response.ProfileImageURLServiceResponse;
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

		Member loginMember = MemberThreadLocal.get();

		Long partnerId = coupleReadService.getPartnerId(loginMember);
		MemberInfoServiceResponse serviceResponse = memberReadService.getMemberInfo(partnerId);

		return ApiResponse.ofSuccess(serviceResponse.toResponse(true));
	}

	@GetMapping("/{member_id}/profile/image")
	public ApiResponse<ProfileImageURLResponse> getProfileImageURL(
		@PathVariable("member_id") Long memberId) {

		Member loginMember = MemberThreadLocal.get();

		Long partnerId = coupleReadService.getPartnerId(loginMember);

		ProfileImageURLServiceResponse serviceResponse = memberReadService.getProfileImageURL(
			loginMember,
			memberId, partnerId);

		return ApiResponse.ofSuccess(serviceResponse.toResponse());
	}

	@GetMapping("/{member_id}/profile/image/presigned-url")
	public ApiResponse<PresignedURLResponse> getPresignedURL(
		@PathVariable("member_id") Long memberId) {

		Member loginMember = MemberThreadLocal.get();

		PresignedURLResponse presingedURL = memberService.getPresignedURLForProfileImage(
			loginMember, memberId);

		return ApiResponse.ofSuccess(presingedURL);
	}

	@PutMapping("/{member_id}/profile/image")
	public ApiResponse<Void> modifyProfileImage(@PathVariable("member_id") Long memberId) {

		Member loginMember = MemberThreadLocal.get();

		memberService.checkAndSaveProfileImage(loginMember, memberId);

		return ApiResponse.ofSuccess();
	}

	@DeleteMapping("/{member_id}/profile/image")
	public ApiResponse<Void> deleteProfileImage(@PathVariable("member_id") Long memberId) {

		Member loginMember = MemberThreadLocal.get();

		memberService.deleteProfileImage(loginMember, memberId);

		return ApiResponse.ofSuccess();
	}

	@GetMapping("/{member_id}/connect")
	public ApiResponse<ConnectionResponse> getConnectionCode(
		@PathVariable("member_id") Long memberId) {

		Member loginMember = MemberThreadLocal.get();

		ConnectionServiceResponse response = coupleService.getConnectionCode(loginMember, memberId);
		return ApiResponse.ofSuccess(ConnectionResponse.from(response));
	}

	@PostMapping("/{member_id}/connect")
	public ApiResponse<Void> connectCouple(@PathVariable("member_id") Long memberId,
		@Valid @RequestBody ConnectionRequest request) {

		Member loginMember = MemberThreadLocal.get();

		CoupleConnectServiceResponse serviceResponse = coupleService.connectCouple(loginMember,
			memberId, request.toConnectionServiceRequest());

		anniversaryService.createAnniversariesForFirstDate(serviceResponse.getCoupleId());
		anniversaryService.createAnniversariesForBirthDay(serviceResponse.getMember1Id());
		anniversaryService.createAnniversariesForBirthDay(serviceResponse.getMember2Id());
		return ApiResponse.ofSuccess();
	}

	@PostMapping("/{member_id}/disconnect")
	public ApiResponse<Void> disconnectCouple(@PathVariable("member_id") Long memberId) {
		final Member member = MemberThreadLocal.get();

		coupleService.disconnectCouple(member, memberId);
		return ApiResponse.ofSuccess();
	}

	@DeleteMapping("/{member_id}")
	public ApiResponse<Void> withdrawal(@PathVariable("member_id") Long memberId) {
		Member member = MemberThreadLocal.get();
		memberService.withdrawal(member, memberId);
		return ApiResponse.ofSuccess();
	}

	@PostMapping("/{member_id}/password")
	public ApiResponse<CheckPasswordResponse> checkPassword(
		@PathVariable("member_id") Long memberId,
		@Valid @RequestBody CheckPasswordRequest request
	) {
		final Member member = MemberThreadLocal.get();
		CheckPasswordServiceResponse checkPasswordServiceResponse = memberService.checkPassword(
			member, memberId, request.toCheckPasswordServiceRequest());
		return ApiResponse.ofSuccess(CheckPasswordResponse.from(checkPasswordServiceResponse));
	}
}
