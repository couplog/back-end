package com.dateplan.dateplan.domain.member.controller.dto.response;

import com.dateplan.dateplan.domain.member.service.dto.request.CheckPasswordServiceResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckPasswordResponse {

	private Boolean checkPassword;

	public static CheckPasswordResponse from(CheckPasswordServiceResponse serviceResponse) {
		return CheckPasswordResponse.builder()
			.checkPassword(serviceResponse.getPasswordMatch())
			.build();
	}
}
