package com.dateplan.dateplan.domain.schedule.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDatesServiceResponse {

	private List<LocalDate> scheduleDates = new ArrayList<>();

	public static ScheduleDatesServiceResponse from(Set<LocalDate> scheduleDateSet) {
		return ScheduleDatesServiceResponse.builder()
			.scheduleDates(scheduleDateSet.stream().toList())
			.build();
	}
}