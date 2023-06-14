package com.dateplan.dateplan.domain.schedule.dto;

import static com.dateplan.dateplan.global.util.ScheduleDateUtil.getNextCycle;

import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.exception.schedule.InvalidDateTimeRangeException;
import com.dateplan.dateplan.global.exception.schedule.InvalidDifferenceDateTimeException;
import com.dateplan.dateplan.global.exception.schedule.InvalidRepeatEndTimeRange;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleServiceRequest {

	private String title;

	private LocalDateTime startDateTime;

	private LocalDateTime endDateTime;

	private String location;

	private String content;

	private RepeatRule repeatRule;

	private LocalDate repeatEndTime;

	private ScheduleServiceRequest(
		String title,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime,
		String location,
		String content,
		RepeatRule repeatRule,
		LocalDate repeatEndTime
	) {
		this.title = title;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.location = location;
		this.content = content;
		this.repeatRule = repeatRule;
		this.repeatEndTime = repeatEndTime;
		setDefaultRepeatEndTime();
		throwIfInvalidDateTimeRange();
		throwIfInvalidRepeatEndTime();
		throwIfInvalidDifferenceDateTime();
	}

	private void setDefaultRepeatEndTime() {
		if (repeatEndTime != null) {
			return;
		}
		repeatEndTime = LocalDate.of(2049, 12, 31);
	}

	private void throwIfInvalidRepeatEndTime() {
		if (repeatEndTime.isAfter(LocalDate.of(2049, 12, 31))) {
			throw new InvalidRepeatEndTimeRange();
		}
		if (repeatEndTime.isBefore(endDateTime.toLocalDate())) {
			throw new InvalidRepeatEndTimeRange();
		}
	}

	private void throwIfInvalidDateTimeRange() {
		if (startDateTime.isAfter(endDateTime)) {
			throw new InvalidDateTimeRangeException();
		}
	}

	private void throwIfInvalidDifferenceDateTime() {
		if (endDateTime.isAfter(getNextCycle(startDateTime, repeatRule, 1))) {
			throw new InvalidDifferenceDateTimeException();
		}
	}

}
