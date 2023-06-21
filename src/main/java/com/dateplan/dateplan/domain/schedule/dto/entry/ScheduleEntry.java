package com.dateplan.dateplan.domain.schedule.dto.entry;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEntry {

	private LocalDate date;
	private Exists exists;

}
