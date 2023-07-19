package com.dateplan.dateplan.domain.member.service.dto.request;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CheckPasswordServiceResponse {

	private Boolean passwordMatch;

	public static CheckPasswordServiceResponse from(boolean passwordMatch) {
		return CheckPasswordServiceResponse.builder()
			.passwordMatch(passwordMatch)
			.build();
	}
}
