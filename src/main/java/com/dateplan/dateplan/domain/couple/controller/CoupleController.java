package com.dateplan.dateplan.domain.couple.controller;

import com.dateplan.dateplan.domain.anniversary.service.AnniversaryService;
import com.dateplan.dateplan.domain.couple.controller.dto.request.FirstDateRequest;
import com.dateplan.dateplan.domain.couple.controller.dto.response.CoupleInfoResponse;
import com.dateplan.dateplan.domain.couple.controller.dto.response.FirstDateResponse;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.domain.couple.service.dto.response.CoupleInfoServiceResponse;
import com.dateplan.dateplan.domain.couple.service.dto.response.FirstDateServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/couples")
public class CoupleController {

	private final AnniversaryService anniversaryService;
	private final CoupleService coupleService;
	private final CoupleReadService coupleReadService;

	@Deprecated
	@GetMapping("/{couple_id}/first-date")
	public ApiResponse<FirstDateResponse> getFirstDate(@PathVariable("couple_id") Long coupleId) {
		FirstDateServiceResponse response = coupleService.getFirstDate(coupleId);
		return ApiResponse.ofSuccess(FirstDateResponse.from(response));
	}

	@PutMapping("/{couple_id}/first-date")
	public ApiResponse<Void> updateFirstDate(@PathVariable("couple_id") Long coupleId,
		@Valid @RequestBody FirstDateRequest request) {

		Member loginMember = MemberThreadLocal.get();

		coupleService.updateFirstDate(loginMember, coupleId, request.toFirstDateServiceRequest());
		anniversaryService.modifyAnniversaryForFirstDate(coupleId, request.getFirstDate());

		return ApiResponse.ofSuccess();
	}

	@GetMapping("/me")
	public ApiResponse<CoupleInfoResponse> getCoupleInfo() {

		Member loginMember = MemberThreadLocal.get();

		CoupleInfoServiceResponse response = coupleReadService.getCoupleInfo(loginMember);
		return ApiResponse.ofSuccess(CoupleInfoResponse.from(response));
	}
}
