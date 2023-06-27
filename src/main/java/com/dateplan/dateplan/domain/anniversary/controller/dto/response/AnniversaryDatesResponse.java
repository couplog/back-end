package com.dateplan.dateplan.domain.anniversary.controller.dto.response;

import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryDatesServiceResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnniversaryDatesResponse {

	private List<LocalDate> anniversaryDates;

	public static AnniversaryDatesResponse from(AnniversaryDatesServiceResponse serviceResponse) {

		return AnniversaryDatesResponse.builder()
			.anniversaryDates(serviceResponse.getAnniversaryDates())
			.build();
	}
}
