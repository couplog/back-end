package com.dateplan.dateplan.domain.schedule.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleResponse {

	private List<ScheduleEntry> schedules;

	public static ScheduleResponse from(ScheduleServiceResponse serviceResponse) {
		return ScheduleResponse.builder()
			.schedules(serviceResponse.getSchedules())
			.build();
	}
}
