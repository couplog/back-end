package com.dateplan.dateplan.domain.dating.controller;

import com.dateplan.dateplan.domain.dating.controller.dto.request.DatingCreateRequest;
import com.dateplan.dateplan.domain.dating.controller.dto.request.DatingUpdateRequest;
import com.dateplan.dateplan.domain.dating.controller.dto.response.DatingDatesResponse;
import com.dateplan.dateplan.domain.dating.controller.dto.response.DatingResponse;
import com.dateplan.dateplan.domain.dating.service.DatingReadService;
import com.dateplan.dateplan.domain.dating.service.DatingService;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingDatesServiceResponse;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingServiceResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
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
	public ApiResponse<Void> createDating(@Valid @RequestBody DatingCreateRequest request) {
		final Member member = MemberThreadLocal.get();
		datingService.createDating(member, request.toDatingCreateServiceRequest());
		return ApiResponse.ofSuccess();
	}

	@GetMapping("/{couple_id}/dating/dates")
	public ApiResponse<DatingDatesResponse> readDatingDates(
		@PathVariable("couple_id") Long coupleId,
		@RequestParam(value = "year", required = false) Integer year,
		@RequestParam(value = "month", required = false) Integer month
	) {
		DatingDatesServiceResponse response = datingReadService.readDatingDates(coupleId, year,
			month);
		return ApiResponse.ofSuccess(DatingDatesResponse.from(response));
	}

	@PutMapping("/{couple_id}/dating/{dating_id}")
	public ApiResponse<Void> updateDating(
		@PathVariable("dating_id") Long datingId,
		@Valid @RequestBody DatingUpdateRequest request
	) {
		datingService.updateDating(datingId, request.toDatingUpdateServiceRequest());
		return ApiResponse.ofSuccess();
	}

	@GetMapping("/{couple_id}/dating")
	public ApiResponse<DatingResponse> readDating(
		@PathVariable("couple_id") Long coupleId,
		@RequestParam("year") Integer year,
		@RequestParam("month") Integer month,
		@RequestParam("day") Integer day
	) {
		DatingServiceResponse response = datingReadService.readDating(coupleId, year, month, day);
		return ApiResponse.ofSuccess(DatingResponse.from(response));
	}

	@DeleteMapping("/{couple_id}/dating/{dating_id}")
	public ApiResponse<Void> deleteDating(
		@PathVariable("dating_id") Long datingId
	) {
		datingService.deleteDating(datingId);
		return ApiResponse.ofSuccess();
	}
}
