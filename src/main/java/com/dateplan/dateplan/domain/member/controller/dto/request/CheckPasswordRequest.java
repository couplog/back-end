package com.dateplan.dateplan.domain.member.controller.dto.request;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_PASSWORD_PATTERN;

import com.dateplan.dateplan.domain.member.service.dto.request.CheckPasswordServiceRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckPasswordRequest {

	@NotNull(message = INVALID_PASSWORD_PATTERN)
	private String password;

	public CheckPasswordServiceRequest toCheckPasswordServiceRequest() {
		return CheckPasswordServiceRequest.builder()
			.password(password)
			.build();
	}
}
