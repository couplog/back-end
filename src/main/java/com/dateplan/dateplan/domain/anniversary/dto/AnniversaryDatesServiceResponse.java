package com.dateplan.dateplan.domain.anniversary.dto;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnniversaryDatesServiceResponse {

	private List<LocalDate> anniversaryDates = new ArrayList<>();

	public static AnniversaryDatesServiceResponse from(List<Anniversary> anniversaries) {

		List<LocalDate> dates = anniversaries.stream()
			.map(Anniversary::getDate)
			.distinct()
			.toList();

		return AnniversaryDatesServiceResponse.builder()
			.anniversaryDates(dates)
			.build();
	}
}
