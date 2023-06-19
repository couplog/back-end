package com.dateplan.dateplan.domain.schedule.controller;

import com.dateplan.dateplan.domain.schedule.dto.ScheduleRequest;
import com.dateplan.dateplan.domain.schedule.service.ScheduleService;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/schedules/{member_id}")
public class ScheduleController {

	private final ScheduleService scheduleService;

	@PostMapping("/{member_id}/schedules")
	public ApiResponse<Void> createSchedule(@PathVariable("member_id") Long memberId,
		@Valid @RequestBody ScheduleRequest request) {
		scheduleService.createSchedule(memberId, request.toScheduleServiceRequest());
		return ApiResponse.ofSuccess();
	}
}
