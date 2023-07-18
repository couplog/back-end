package com.dateplan.dateplan.domain.member.controller.dto.request;

import com.dateplan.dateplan.domain.member.service.dto.request.UpdatePasswordServiceRequest;
import com.dateplan.dateplan.global.constant.InputPattern;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePasswordRequest {

	@NotNull(message = DetailMessage.INVALID_PASSWORD_PATTERN)
	@Pattern(regexp = InputPattern.PASSWORD_PATTERN, message = DetailMessage.INVALID_PASSWORD_PATTERN)
	private String password;

	public UpdatePasswordServiceRequest toUpdatePasswordServiceRequest() {
		return UpdatePasswordServiceRequest.builder()
			.password(password)
			.build();
	}
}
