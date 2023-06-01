package com.dateplan.dateplan.domain.member.dto;

import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginRequest {

	@Pattern(regexp = "^010\\d{4}\\d{4}$", message = DetailMessage.INVALID_PHONE_PATTERN)
	@NotEmpty(message = DetailMessage.INVALID_PHONE_PATTERN)
	private String phone;
	@NotNull
	private String password;

	public LoginServiceRequest toServiceRequest() {
		return LoginServiceRequest.builder()
			.phone(this.phone)
			.password(this.password)
			.build();
	}
}
