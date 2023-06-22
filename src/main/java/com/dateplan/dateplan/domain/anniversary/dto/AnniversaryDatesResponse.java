package com.dateplan.dateplan.domain.anniversary.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnniversaryDatesResponse {

	private List<LocalDate> anniversaryDates = new ArrayList<>();

	public static AnniversaryDatesResponse from(AnniversaryDatesServiceResponse serviceResponse) {

		return AnniversaryDatesResponse.builder()
			.anniversaryDates(serviceResponse.getAnniversaryDates())
			.build();
	}
}
