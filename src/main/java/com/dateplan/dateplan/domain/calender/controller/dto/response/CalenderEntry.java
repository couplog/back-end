package com.dateplan.dateplan.domain.calender.controller.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CalenderEntry {

	private LocalDate date;
	private List<String> events;
}
