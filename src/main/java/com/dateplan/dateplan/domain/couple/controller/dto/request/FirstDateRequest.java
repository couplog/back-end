package com.dateplan.dateplan.domain.couple.controller.dto.request;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DATE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_FIRST_DATE_RANGE;

import com.dateplan.dateplan.domain.couple.service.dto.request.FirstDateServiceRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class FirstDateRequest {

	@NotNull(message = INVALID_DATE_PATTERN)
	@DateTimeFormat(iso = ISO.DATE)
	@PastOrPresent(message = INVALID_FIRST_DATE_RANGE)
	private LocalDate firstDate;

	public FirstDateServiceRequest toFirstDateServiceRequest() {
		return FirstDateServiceRequest.builder()
			.firstDate(firstDate)
			.build();
	}
}
