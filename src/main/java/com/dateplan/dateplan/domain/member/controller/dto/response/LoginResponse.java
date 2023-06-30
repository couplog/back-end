package com.dateplan.dateplan.domain.member.controller.dto.response;

import com.dateplan.dateplan.domain.member.service.dto.response.LoginServiceResponse;
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
