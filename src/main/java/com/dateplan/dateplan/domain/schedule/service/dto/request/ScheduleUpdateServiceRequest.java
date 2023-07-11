package com.dateplan.dateplan.domain.schedule.service.dto.request;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.global.constant.RepeatRule;
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

	public SchedulePattern toSchedulePattern(Member member) {
		return SchedulePattern.builder()
			.repeatStartDate(startDateTime.toLocalDate())
			.repeatEndDate(endDateTime.toLocalDate())
			.repeatRule(RepeatRule.N)
			.member(member)
			.build();
	}

	private void throwIfInvalidDateTimeRange() {
		if (startDateTime.isAfter(endDateTime)) {
			throw new InvalidDateTimeRangeException();
		}
	}
}
