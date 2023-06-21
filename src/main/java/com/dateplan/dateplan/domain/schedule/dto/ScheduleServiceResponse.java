package com.dateplan.dateplan.domain.schedule.dto;

import com.dateplan.dateplan.domain.schedule.dto.entry.ScheduleEntry;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleServiceResponse {

	private List<ScheduleEntry> schedules;

	public ScheduleResponse toScheduleResponse() {
		return ScheduleResponse.builder()
			.schedules(schedules)
			.build();
	}
}