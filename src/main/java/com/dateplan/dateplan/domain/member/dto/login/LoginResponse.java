package com.dateplan.dateplan.domain.member.dto.login;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoginResponse {

	private Boolean isConnected;
}
