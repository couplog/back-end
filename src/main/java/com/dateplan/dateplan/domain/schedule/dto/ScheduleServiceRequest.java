package com.dateplan.dateplan.domain.schedule.dto;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.exception.schedule.InvalidDateTimeRangeException;
import com.dateplan.dateplan.global.exception.schedule.InvalidRepeatEndTimeRange;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

	public SchedulePattern toSchedulePatternEntity(Member member) {
		return SchedulePattern.builder()
			.repeatStartDate(startDateTime.toLocalDate())
			.repeatEndDate(repeatEndTime)
			.member(member)
			.repeatRule(repeatRule)
			.build();
	}

	public Schedule toScheduleEntity(LocalDateTime now, SchedulePattern schedulePattern) {
		long diff = ChronoUnit.SECONDS.between(startDateTime, endDateTime);
		return Schedule.builder()
			.startDateTime(now)
			.endDateTime(now.plusSeconds(diff))
			.title(title)
			.content(content)
			.location(location)
			.schedulePattern(schedulePattern)
			.build();
	}
}
