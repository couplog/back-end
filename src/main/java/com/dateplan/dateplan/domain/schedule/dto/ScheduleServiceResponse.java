package com.dateplan.dateplan.domain.schedule.dto;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ScheduleServiceResponse {

	List<ScheduleEntry> schedules;

	public static ScheduleServiceResponse from(List<Schedule> schedules) {
		return ScheduleServiceResponse.builder()
			.schedules(schedules.stream()
			.map(ScheduleEntry::from)
			.toList())
			.build();
	}
}
