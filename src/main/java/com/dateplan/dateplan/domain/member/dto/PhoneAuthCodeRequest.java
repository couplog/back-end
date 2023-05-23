package com.dateplan.dateplan.domain.member.dto;

import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class PhoneAuthCodeRequest {

	@Pattern(regexp = "^010\\d{4}\\d{4}$", message = DetailMessage.INVALID_PHONE_PATTERN)
	private String phone;

	@Pattern(regexp = "^\\d{6}$", message = DetailMessage.INVALID_PHONE_AUTH_CODE_PATTERN)
	private String code;

	public PhoneAuthCodeServiceRequest toServiceRequest() {
		return new PhoneAuthCodeServiceRequest(this.phone, this.code);
	}
}
