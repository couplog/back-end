package com.dateplan.dateplan.domain.schedule.controller.dto.response;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.global.constant.RepeatRule;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleEntry {

	private Long scheduleId;
	private LocalDateTime startDateTime;
	private LocalDateTime endDateTime;
	private String title;
	private String content;
	private String location;
	private RepeatRule repeatRule;

	public static ScheduleEntry from(Schedule schedule) {
		return ScheduleEntry.builder()
			.scheduleId(schedule.getId())
			.startDateTime(schedule.getStartDateTime())
			.endDateTime(schedule.getEndDateTime())
			.title(schedule.getTitle())
			.content(schedule.getContent())
			.location(schedule.getLocation())
			.repeatRule(schedule.getSchedulePattern().getRepeatRule())
			.build();
	}
}
