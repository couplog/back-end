package com.dateplan.dateplan.domain.member.service.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginServiceRequest {

	private String phone;
	private String password;
}
