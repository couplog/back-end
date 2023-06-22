package com.dateplan.dateplan.domain.anniversary.controller;

import com.dateplan.dateplan.domain.anniversary.dto.AnniversaryCreateRequest;
import com.dateplan.dateplan.domain.anniversary.dto.AnniversaryDatesResponse;
import com.dateplan.dateplan.domain.anniversary.dto.AnniversaryDatesServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryReadService;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/couples/{couple_id}/anniversary")
public class AnniversaryController {

	private final AnniversaryService anniversaryService;
	private final AnniversaryReadService anniversaryReadService;

	@ResponseStatus(value = HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<Void> createAnniversary(@PathVariable("couple_id") Long coupleId,
		@RequestBody @Valid AnniversaryCreateRequest request) {

		anniversaryService.createAnniversaries(MemberThreadLocal.get(), coupleId,
			request.toServiceRequest());

		return ApiResponse.ofSuccess();
	}

	@GetMapping
	public ApiResponse<AnniversaryDatesResponse> readAnniversaryDates(
		@PathVariable("couple_id") Long coupleId,
		@RequestParam("year") Integer year,
		@RequestParam("month") Integer month) {

		Member loginMember = MemberThreadLocal.get();

		AnniversaryDatesServiceResponse serviceResponse = anniversaryReadService.readAnniversaryDates(
			loginMember, coupleId, year, month);

		return ApiResponse.ofSuccess(AnniversaryDatesResponse.from(serviceResponse));
	}
}
