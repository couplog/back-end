package com.dateplan.dateplan.domain.member.dto.login;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoginResponse {

	private Boolean isConnected;

	public static LoginResponse from(LoginServiceResponse response) {
		return LoginResponse.builder()
			.isConnected(response.getIsConnected())
			.build();
	}
}
