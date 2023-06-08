package com.dateplan.dateplan.domain.member.dto.login;

import com.dateplan.dateplan.domain.member.dto.AuthToken;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginServiceResponse {

	private AuthToken authToken;
	private Boolean isConnected;

	public LoginResponse toLoginResponse() {
		return LoginResponse.builder()
			.isConnected(isConnected)
			.build();
	}
}
