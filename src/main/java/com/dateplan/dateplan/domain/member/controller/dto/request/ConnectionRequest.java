package com.dateplan.dateplan.domain.member.controller.dto.request;

import static com.dateplan.dateplan.global.constant.InputPattern.CONNECTION_CODE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CONNECTION_CODE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DATE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_FIRST_DATE_RANGE;

import com.dateplan.dateplan.domain.member.service.dto.request.ConnectionServiceRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionRequest {

	@NotNull(message = INVALID_CONNECTION_CODE_PATTERN)
	@Pattern(regexp = CONNECTION_CODE_PATTERN, message = INVALID_CONNECTION_CODE_PATTERN)
	private String connectionCode;

	@NotNull(message = INVALID_DATE_PATTERN)
	@DateTimeFormat(iso = ISO.DATE)
	@PastOrPresent(message = INVALID_FIRST_DATE_RANGE)
	private LocalDate firstDate;

	public ConnectionServiceRequest toConnectionServiceRequest() {
		return ConnectionServiceRequest.builder()
			.connectionCode(connectionCode)
			.firstDate(firstDate)
			.build();
	}

}
