package com.dateplan.dateplan.domain.schedule.controller;

import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleDatesResponse;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleDatesServiceResponse;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleRequest;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleResponse;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceResponse;
import com.dateplan.dateplan.domain.schedule.service.ScheduleReadService;
import com.dateplan.dateplan.domain.schedule.service.ScheduleService;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
	private final CoupleReadService coupleReadService;

	@PostMapping("/{member_id}/schedules")
	public ApiResponse<Void> createSchedule(@PathVariable("member_id") Long memberId,
		@Valid @RequestBody ScheduleRequest request) {
		scheduleService.createSchedule(memberId, request.toScheduleServiceRequest());
		return ApiResponse.ofSuccess();
	}

	@GetMapping("/{member_id}/schedules/dates")
	public ApiResponse<ScheduleDatesResponse> readScheduleDates(
		@PathVariable("member_id") Long memberId,
		@RequestParam(value = "year", required = false) Integer year,
		@RequestParam(value = "month", required = false) Integer month
	) {
		ScheduleDatesServiceResponse scheduleDatesServiceResponse = scheduleReadService
			.readScheduleDates(memberId, year, month);

		return ApiResponse.ofSuccess(ScheduleDatesResponse.from(scheduleDatesServiceResponse));
	}

	@GetMapping("/{member_id}/schedules")
	public ApiResponse<ScheduleResponse> readSchedules(
		@PathVariable("member_id") Long memberId,
		@RequestParam(value = "year") Integer year,
		@RequestParam(value = "month") Integer month,
		@RequestParam(value = "day") Integer day
	) {
		final Member member = MemberThreadLocal.get();
		Long coupleId = coupleReadService.getPartnerId(member);
		ScheduleServiceResponse response = scheduleReadService.readSchedules(memberId, coupleId,
			member, year, month, day);
		return ApiResponse.ofSuccess(ScheduleResponse.from(response));
	}

	@DeleteMapping("/{member_id}/schedules/{schedule_id}")
	public ApiResponse<Void> deleteSchedule(
		@PathVariable("member_id") Long memberId,
		@PathVariable("schedule_id") Long scheduleId,
		@RequestParam(value = "deleteRepeat", defaultValue = "false") Boolean deleteRepeat
	) {
		Member member = MemberThreadLocal.get();
		scheduleService.deleteSchedule(memberId, scheduleId, member, deleteRepeat);
		return ApiResponse.ofSuccess();
	}
}
