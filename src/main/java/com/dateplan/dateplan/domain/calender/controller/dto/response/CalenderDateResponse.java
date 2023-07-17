package com.dateplan.dateplan.domain.calender.controller.dto.response;

import com.dateplan.dateplan.domain.calender.service.dto.response.CalenderDateServiceResponse;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CalenderDateResponse {

	private List<CalenderEntry> schedules;

	public static CalenderDateResponse from(CalenderDateServiceResponse response) {
		return CalenderDateResponse.builder()
			.schedules(response.getSchedules())
			.build();
	}
}
