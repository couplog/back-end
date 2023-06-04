package com.dateplan.dateplan.domain.member.dto;

import static com.dateplan.dateplan.global.constant.InputPattern.CONNECTION_CODE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CONNECTION_CODE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DATE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_FIRST_DATE_RANGE;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
public class ConnectionRequest {

	@NotNull
	@Pattern(regexp = CONNECTION_CODE_PATTERN, message = INVALID_CONNECTION_CODE_PATTERN)
	private String connectionCode;

	@NotNull(message = INVALID_DATE_PATTERN)
	@DateTimeFormat(iso = ISO.DATE)
	@Past(message = INVALID_FIRST_DATE_RANGE)
	private LocalDate firstDate;

	public ConnectionServiceRequest toConnectionServiceRequest() {
		return ConnectionServiceRequest.builder()
			.connectionCode(connectionCode)
			.firstDate(firstDate)
			.build();
	}

}
