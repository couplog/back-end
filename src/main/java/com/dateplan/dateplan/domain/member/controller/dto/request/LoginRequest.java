package com.dateplan.dateplan.domain.member.controller.dto.request;

import com.dateplan.dateplan.domain.member.service.dto.request.LoginServiceRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
