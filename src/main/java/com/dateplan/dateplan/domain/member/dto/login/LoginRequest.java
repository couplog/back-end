package com.dateplan.dateplan.domain.member.dto.login;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginRequest {

	private String phone;
	private String password;

	public LoginServiceRequest toServiceRequest() {
		return LoginServiceRequest.builder()
			.phone(this.phone)
			.password(this.password)
			.build();
	}
}
