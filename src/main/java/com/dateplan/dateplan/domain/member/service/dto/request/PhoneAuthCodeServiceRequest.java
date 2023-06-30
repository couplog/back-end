package com.dateplan.dateplan.domain.member.service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhoneAuthCodeServiceRequest {

	private String phone;
	private String code;
}
