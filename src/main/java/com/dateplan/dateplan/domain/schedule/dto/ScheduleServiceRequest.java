package com.dateplan.dateplan.domain.schedule.dto;

import static com.dateplan.dateplan.global.util.ScheduleDateUtil.*;

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

	public void setDefaultRepeatEndTime() {
		this.repeatEndTime = LocalDate.of(2049, 12, 31);
	}

	public void checkValidation() {
		throwIfInvalidDateTimeRange();
		throwIfInvalidRepeatEndTime();
		throwIfInvalidDifferenceDateTime();
	}

	private void throwIfInvalidRepeatEndTime() {
		if (repeatEndTime.isAfter(LocalDate.of(2049, 12, 31))) {
			throw new InvalidRepeatEndTimeRange();
		}
		if (repeatEndTime.isBefore(startDateTime.toLocalDate())) {
			throw new InvalidRepeatEndTimeRange();
		}
	}

	private void throwIfInvalidDateTimeRange() {
		if (startDateTime.isAfter(endDateTime)) {
			throw new InvalidDateTimeRangeException();
		}
	}

	private void throwIfInvalidDifferenceDateTime() {
		if (endDateTime.isAfter(getNextCycle(startDateTime, repeatRule))) {
			throw new InvalidDifferenceDateTimeException();
		}
	}

}
