package com.dateplan.dateplan.domain.dating.controller;

import com.dateplan.dateplan.domain.dating.controller.dto.request.DatingCreateRequest;
import com.dateplan.dateplan.domain.dating.controller.dto.response.DatingDatesResponse;
import com.dateplan.dateplan.domain.dating.service.DatingReadService;
import com.dateplan.dateplan.domain.dating.service.DatingService;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingDatesServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/couples")
public class DatingController {

	private final DatingService datingService;
	private final DatingReadService datingReadService;

	@ResponseStatus(value = HttpStatus.CREATED)
	@PostMapping("/{couple_id}/dating")
	public ApiResponse<Void> createDating(
		@PathVariable("couple_id") Long coupleId,
		@Valid @RequestBody DatingCreateRequest request
	) {
		final Member member = MemberThreadLocal.get();
		datingService.createDating(member, coupleId, request.toDatingCreateServiceRequest());
		return ApiResponse.ofSuccess();
	}

	@GetMapping("/{couple_id}/dating/dates")
	public ApiResponse<DatingDatesResponse> readDatingDates(
		@PathVariable("couple_id") Long coupleId,
		@RequestParam(value = "year", required = false) Integer year,
		@RequestParam(value = "month", required = false) Integer month
	) {
		final Member member = MemberThreadLocal.get();
		DatingDatesServiceResponse response = datingReadService.readDatingDates(
			member, coupleId, year, month);
		return ApiResponse.ofSuccess(DatingDatesResponse.from(response));
	}

	@DeleteMapping("/{couple_id}/dating/{dating_id}")
	public ApiResponse<Void> deleteDating(
		@PathVariable("couple_id") Long coupleId,
		@PathVariable("dating_id") Long datingId
	) {
		final Member member = MemberThreadLocal.get();
		datingService.deleteDating(member, coupleId, datingId);
		return ApiResponse.ofSuccess();
	}
}
