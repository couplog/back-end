package com.dateplan.dateplan.domain.member.dto.signup;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhoneAuthCodeServiceRequest {

	private String phone;
	private String code;
}
