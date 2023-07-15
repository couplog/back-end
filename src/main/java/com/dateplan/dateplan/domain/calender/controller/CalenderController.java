package com.dateplan.dateplan.domain.calender.controller;

import com.dateplan.dateplan.domain.calender.controller.dto.response.CalenderDateResponse;
import com.dateplan.dateplan.domain.calender.service.CalenderReadService;
import com.dateplan.dateplan.domain.calender.service.dto.response.CalenderDateServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/{member_id}")
public class CalenderController {

	private final CalenderReadService calenderReadService;

	@GetMapping("/calender/date")
	public ApiResponse<CalenderDateResponse> readCalenderDate(
		@PathVariable(value = "member_id") Long memberId,
		@RequestParam(value = "year") Integer year,
		@RequestParam(value = "month") Integer month
	) {
		final Member member = MemberThreadLocal.get();
		CalenderDateServiceResponse response = calenderReadService.readCalenderDates(
			member, memberId, year, month);
		return ApiResponse.ofSuccess(CalenderDateResponse.from(response));
	}
}
