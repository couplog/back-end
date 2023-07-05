package com.dateplan.dateplan.domain.dating.controller.dto.response;

import com.dateplan.dateplan.domain.dating.service.dto.response.DatingDatesServiceResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DatingDatesResponse {

	private List<LocalDate> datingDates;

	public static DatingDatesResponse from(DatingDatesServiceResponse response) {
		return DatingDatesResponse.builder()
			.datingDates(response.getDatingDates())
			.build();
	}
}
