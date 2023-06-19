package com.dateplan.dateplan.domain.schedule.dto;

import java.time.LocalDate;
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

	private List<LocalDate> memberSchedules;
	private List<LocalDate> partnerSchedules;
	private List<LocalDate> dateSchedules;

	public ScheduleResponse toScheduleResponse() {
		return ScheduleResponse.builder()
			.memberSchedules(memberSchedules)
			.partnerSchedules(partnerSchedules)
			.dateSchedules(dateSchedules)
			.build();
	}
}