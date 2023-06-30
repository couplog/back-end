package com.dateplan.dateplan.domain.schedule.controller.dto.request;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_REPEAT_RULE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_CONTENT;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_LOCATION;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_TIME;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_TITLE;

import com.dateplan.dateplan.domain.schedule.service.dto.request.ScheduleServiceRequest;
import com.dateplan.dateplan.global.constant.InputPattern;
import com.dateplan.dateplan.global.constant.RepeatRule;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

	@NotNull(message = INVALID_SCHEDULE_TITLE)
	@Size(max = 15, message = INVALID_SCHEDULE_TITLE)
	private String title;

	@NotNull(message = INVALID_SCHEDULE_TIME)
	@DateTimeFormat(pattern = InputPattern.DATE_TIME_PATTERN)
	private LocalDateTime startDateTime;

	@NotNull(message = INVALID_SCHEDULE_TIME)
	@DateTimeFormat(pattern = InputPattern.DATE_TIME_PATTERN)
	private LocalDateTime endDateTime;

	@Size(max = 20, message = INVALID_SCHEDULE_LOCATION)
	private String location;

	@Size(max = 100, message = INVALID_SCHEDULE_CONTENT)
	private String content;

	@NotNull(message = INVALID_REPEAT_RULE)
	private RepeatRule repeatRule;

	@DateTimeFormat(pattern = InputPattern.DATE_PATTERN)
	private LocalDate repeatEndTime;

	public ScheduleServiceRequest toScheduleServiceRequest() {
		return ScheduleServiceRequest.builder()
			.title(title)
			.startDateTime(startDateTime)
			.endDateTime(endDateTime)
			.location(location)
			.content(content)
			.repeatRule(repeatRule)
			.repeatEndTime(repeatEndTime)
			.build();
	}
}
