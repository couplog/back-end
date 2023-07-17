package com.dateplan.dateplan.domain.calender.service.dto.response;

import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryDatesServiceResponse;
import com.dateplan.dateplan.domain.calender.controller.dto.response.CalenderEntry;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingDatesServiceResponse;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleDatesServiceResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CalenderDateServiceResponse {

	List<CalenderEntry> schedules;

	public static CalenderDateServiceResponse of(
		DatingDatesServiceResponse datingDates,
		ScheduleDatesServiceResponse myScheduleDates,
		ScheduleDatesServiceResponse partnerScheduleDates,
		AnniversaryDatesServiceResponse anniversaryDates
	) {
		Map<LocalDate, List<String>> dateStringMap = new TreeMap<>();
		addDatesToList(dateStringMap, datingDates.getDatingDates(), "datingSchedule");
		addDatesToList(dateStringMap, myScheduleDates.getScheduleDates(), "mySchedule");
		addDatesToList(dateStringMap, partnerScheduleDates.getScheduleDates(), "partnerSchedule");
		addDatesToList(dateStringMap, anniversaryDates.getAnniversaryDates(), "anniversary");

		return CalenderDateServiceResponse.builder()
			.schedules(dateStringMap.entrySet()
				.stream()
				.map(k -> CalenderEntry.builder()
					.date(k.getKey())
					.events(k.getValue())
					.build())
				.toList())
			.build();
	}

	private static void addDatesToList(
		Map<LocalDate, List<String>> map,
		List<LocalDate> dates,
		String value
	) {
		for (LocalDate date : dates) {
			map.computeIfAbsent(date, k -> new ArrayList<>()).add(value);
		}
	}
}
