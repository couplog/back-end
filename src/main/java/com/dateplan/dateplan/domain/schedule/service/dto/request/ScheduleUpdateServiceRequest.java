package com.dateplan.dateplan.domain.schedule.service.dto.request;

import com.dateplan.dateplan.global.exception.schedule.InvalidDateTimeRangeException;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleUpdateServiceRequest {

	private String title;

	private LocalDateTime startDateTime;

	private LocalDateTime endDateTime;

	private String location;

	private String content;

	public ScheduleUpdateServiceRequest(
		String title,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime,
		String location,
		String content
	) {
		this.title = title;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.location = location;
		this.content = content;
		throwIfInvalidDateTimeRange();
	}

	private void throwIfInvalidDateTimeRange() {
		if (startDateTime.isAfter(endDateTime)) {
			throw new InvalidDateTimeRangeException();
		}
	}
}
