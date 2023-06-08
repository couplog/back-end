package com.dateplan.dateplan.domain.member.dto.login;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginServiceRequest {

	private String phone;
	private String password;
}
