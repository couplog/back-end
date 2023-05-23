package com.dateplan.dateplan.domain.sms.type;

import lombok.Getter;

@Getter
public enum SmsType {

	PHONE_AUTHENTICATION("휴대전화 인증");

	private final String name;

	SmsType(String name) {
		this.name = name;
	}
}
