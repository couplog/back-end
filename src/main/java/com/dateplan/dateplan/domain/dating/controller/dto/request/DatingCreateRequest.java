package com.dateplan.dateplan.domain.dating.controller.dto.request;

import com.dateplan.dateplan.domain.dating.service.dto.request.DatingCreateServiceRequest;
import com.dateplan.dateplan.global.constant.InputPattern;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.validator.BeforeCalenderEndTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DatingCreateRequest {

	@NotNull(message = DetailMessage.INVALID_SCHEDULE_TITLE)
	@Size(min = 1, max = 15, message = DetailMessage.INVALID_SCHEDULE_TITLE)
	private String title;

	@Size(max = 20, message = DetailMessage.INVALID_SCHEDULE_LOCATION)
	private String location;

	@Size(max = 80, message = DetailMessage.INVALID_SCHEDULE_CONTENT)
	private String content;

	@NotNull
	@DateTimeFormat(pattern = InputPattern.DATE_TIME_PATTERN)
	private LocalDateTime startDateTime;

	@NotNull
	@DateTimeFormat(pattern = InputPattern.DATE_TIME_PATTERN)
	@BeforeCalenderEndTime
	private LocalDateTime endDateTime;

	public DatingCreateServiceRequest toDatingCreateServiceRequest() {
		return DatingCreateServiceRequest.builder()
			.title(title)
			.location(location)
			.content(content)
			.startDateTime(startDateTime)
			.endDateTime(endDateTime)
			.build();
	}
}
