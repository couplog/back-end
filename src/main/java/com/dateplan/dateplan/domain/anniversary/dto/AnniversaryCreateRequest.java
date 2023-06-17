package com.dateplan.dateplan.domain.anniversary.dto;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_ANNIVERSARY_CONTENT;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_ANNIVERSARY_REPEAT_RULE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_ANNIVERSARY_TITLE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DATE_PATTERN;

import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.global.validator.BeforeCalenderEndTime;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnniversaryCreateRequest {

	@NotNull(message = INVALID_ANNIVERSARY_TITLE)
	@Size(min = 2, max = 15, message = INVALID_ANNIVERSARY_TITLE)
	private String title;

	@Max(value = 100, message = INVALID_ANNIVERSARY_CONTENT)
	private String content;

	@NotNull(message = INVALID_ANNIVERSARY_REPEAT_RULE)
	private AnniversaryRepeatRule repeatRule;

	@DateTimeFormat(iso = ISO.DATE)
	@NotNull(message = INVALID_DATE_PATTERN)
	@BeforeCalenderEndTime
	private LocalDate date;

	public AnniversaryCreateServiceRequest toServiceRequest(){

		return AnniversaryCreateServiceRequest.builder()
			.title(title)
			.content(content)
			.repeatRule(repeatRule)
			.date(date)
			.build();
	}
}
