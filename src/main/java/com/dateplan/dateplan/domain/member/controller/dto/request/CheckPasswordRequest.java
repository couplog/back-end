package com.dateplan.dateplan.domain.member.controller.dto.request;

import com.dateplan.dateplan.domain.member.service.dto.request.CheckPasswordServiceRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckPasswordRequest {

	private String password;

	public CheckPasswordServiceRequest toCheckPasswordServiceRequest() {
		return CheckPasswordServiceRequest.builder()
			.password(password)
			.build();
	}
}
