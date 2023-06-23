package com.dateplan.dateplan.domain.schedule.controller;

import com.dateplan.dateplan.domain.schedule.dto.ScheduleDatesResponse;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleDatesServiceResponse;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleRequest;
import com.dateplan.dateplan.domain.schedule.service.ScheduleReadService;
import com.dateplan.dateplan.domain.schedule.service.ScheduleService;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class ScheduleController {

	private final ScheduleService scheduleService;
	private final ScheduleReadService scheduleReadService;

	@PostMapping("/{member_id}/schedules")
	public ApiResponse<Void> createSchedule(@PathVariable("member_id") Long memberId,
		@Valid @RequestBody ScheduleRequest request) {
		scheduleService.createSchedule(memberId, request.toScheduleServiceRequest());
		return ApiResponse.ofSuccess();
	}

	@GetMapping("/{member_id}/schedules/dates")
	public ApiResponse<ScheduleDatesResponse> readSchedule(
		@PathVariable("member_id") Long memberId,
		@RequestParam(value = "year", required = false) Integer year,
		@RequestParam(value = "month", required = false) Integer month
	) {
		ScheduleDatesServiceResponse scheduleDatesServiceResponse = scheduleReadService
			.readSchedule(memberId, year, month);

		return ApiResponse.ofSuccess(ScheduleDatesResponse.from(scheduleDatesServiceResponse));
	}
}
