package com.dateplan.dateplan.domain.schedule.controller.dto.response;

import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleDatesServiceResponse;
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
public class ScheduleDatesResponse {
	private List<LocalDate> scheduleDates;

	public static ScheduleDatesResponse from(ScheduleDatesServiceResponse serviceResponse) {
		return ScheduleDatesResponse.builder()
			.scheduleDates(serviceResponse.getScheduleDates())
			.build();
	}
}
