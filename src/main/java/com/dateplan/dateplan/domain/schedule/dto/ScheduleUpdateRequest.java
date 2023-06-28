package com.dateplan.dateplan.domain.schedule.dto;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_CONTENT;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_LOCATION;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_TIME;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_TITLE;

import com.dateplan.dateplan.global.constant.InputPattern;
import com.dateplan.dateplan.global.validator.BeforeCalenderEndTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ScheduleUpdateRequest {

	@NotNull(message = INVALID_SCHEDULE_TITLE)
	@Size(min = 1, max = 15, message = INVALID_SCHEDULE_TITLE)
	private String title;

	@NotNull(message = INVALID_SCHEDULE_TIME)
	@DateTimeFormat(pattern = InputPattern.DATE_TIME_PATTERN)
	private LocalDateTime startDateTime;

	@NotNull(message = INVALID_SCHEDULE_TIME)
	@DateTimeFormat(pattern = InputPattern.DATE_TIME_PATTERN)
	@BeforeCalenderEndTime
	private LocalDateTime endDateTime;

	@Size(max = 20, message = INVALID_SCHEDULE_LOCATION)
	private String location;

	@Size(max = 100, message = INVALID_SCHEDULE_CONTENT)
	private String content;

	public ScheduleUpdateServiceRequest toScheduleUpdateServiceRequest() {
		return ScheduleUpdateServiceRequest.builder()
			.title(title)
			.startDateTime(startDateTime)
			.endDateTime(endDateTime)
			.location(location)
			.content(content)
			.build();
	}
}
