package com.dateplan.dateplan.domain.anniversary.controller;

import com.dateplan.dateplan.domain.anniversary.controller.dto.request.AnniversaryCreateRequest;
import com.dateplan.dateplan.domain.anniversary.controller.dto.request.AnniversaryModifyRequest;
import com.dateplan.dateplan.domain.anniversary.controller.dto.response.AnniversaryDatesResponse;
import com.dateplan.dateplan.domain.anniversary.controller.dto.response.AnniversaryListResponse;
import com.dateplan.dateplan.domain.anniversary.controller.dto.response.ComingAnniversaryListResponse;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryReadService;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryService;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryDatesServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryListServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.ComingAnniversaryListServiceResponse;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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
@RequestMapping("/api/couples/{couple_id}/anniversary")
public class AnniversaryController {

	private final AnniversaryService anniversaryService;
	private final AnniversaryReadService anniversaryReadService;

	@ResponseStatus(value = HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<Void> createAnniversary(@PathVariable("couple_id") Long coupleId,
		@RequestBody @Valid AnniversaryCreateRequest request) {

		anniversaryService.createAnniversaries(coupleId, request.toServiceRequest());

		return ApiResponse.ofSuccess();
	}

	@GetMapping
	public ApiResponse<AnniversaryListResponse> readAnniversaries(
		@PathVariable("couple_id") Long coupleId,
		@RequestParam(value = "year", required = false) Integer year,
		@RequestParam(value = "month", required = false) Integer month,
		@RequestParam(value = "day", required = false) Integer day,
		@RequestParam(value = "onlyRepeatStarted", required = false) boolean onlyRepeatStarted
	) {

		AnniversaryListServiceResponse serviceResponse = anniversaryReadService.readAnniversaries(
			coupleId, year, month, day, onlyRepeatStarted);

		return ApiResponse.ofSuccess(AnniversaryListResponse.from(serviceResponse));
	}

	@GetMapping("/dates")
	public ApiResponse<AnniversaryDatesResponse> readAnniversaryDates(
		@PathVariable("couple_id") Long coupleId,
		@RequestParam("year") Integer year,
		@RequestParam("month") Integer month) {

		AnniversaryDatesServiceResponse serviceResponse = anniversaryReadService.readAnniversaryDates(
			coupleId, year, month);

		return ApiResponse.ofSuccess(AnniversaryDatesResponse.from(serviceResponse));
	}

	@GetMapping("/coming")
	public ApiResponse<ComingAnniversaryListResponse> readComingAnniversaries(
		@PathVariable("couple_id") Long coupleId,
		@RequestParam(value = "size", defaultValue = "3") Integer size,
		@DateTimeFormat(iso = ISO.DATE) LocalDate startDate
	) {

		ComingAnniversaryListServiceResponse serviceResponse = anniversaryReadService.readComingAnniversaries(
			coupleId, startDate, size);

		return ApiResponse.ofSuccess(ComingAnniversaryListResponse.from(serviceResponse));
	}

	@PutMapping("/{anniversary_id}")
	public ApiResponse<Void> modifyAnniversary(
		@PathVariable("anniversary_id") Long anniversaryId,
		@RequestBody @Valid AnniversaryModifyRequest request
	) {

		anniversaryService.modifyAnniversary(anniversaryId, request.toServiceRequest(), false);

		return ApiResponse.ofSuccess();
	}

	@DeleteMapping("/{anniversary_id}")
	public ApiResponse<Void> deleteAnniversary(
		@PathVariable("anniversary_id") Long anniversaryId
	) {

		anniversaryService.deleteAnniversary(anniversaryId);

		return ApiResponse.ofSuccess();
	}
}
