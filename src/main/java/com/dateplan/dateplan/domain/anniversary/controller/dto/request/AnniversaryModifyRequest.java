package com.dateplan.dateplan.domain.anniversary.controller.dto.request;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_ANNIVERSARY_CONTENT;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_ANNIVERSARY_TITLE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DATE_PATTERN;

import com.dateplan.dateplan.domain.anniversary.service.dto.request.AnniversaryModifyServiceRequest;
import com.dateplan.dateplan.global.validator.BeforeCalenderEndTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@Builder
public class AnniversaryModifyRequest {

	@NotNull(message = INVALID_ANNIVERSARY_TITLE)
	@Size(min = 2, max = 15, message = INVALID_ANNIVERSARY_TITLE)
	private String title;

	@Size(max = 80, message = INVALID_ANNIVERSARY_CONTENT)
	private String content;

	@DateTimeFormat(iso = ISO.DATE)
	@NotNull(message = INVALID_DATE_PATTERN)
	@BeforeCalenderEndTime
	private LocalDate date;

	public AnniversaryModifyServiceRequest toServiceRequest(){

		return AnniversaryModifyServiceRequest.builder()
			.title(title)
			.content(content)
			.date(date)
			.build();
	}
}
