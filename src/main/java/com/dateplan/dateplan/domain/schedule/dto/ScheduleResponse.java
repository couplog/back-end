package com.dateplan.dateplan.domain.schedule.dto;

import com.dateplan.dateplan.domain.schedule.dto.entry.ScheduleEntry;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

	private List<ScheduleEntry> schedules;
}
