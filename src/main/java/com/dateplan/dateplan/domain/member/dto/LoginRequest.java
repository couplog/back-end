package com.dateplan.dateplan.domain.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LoginRequest {

	@NotNull
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
