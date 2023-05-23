package com.dateplan.dateplan.domain.member.dto;

import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class PhoneRequest {

	@Pattern(regexp = "^010\\d{4}\\d{4}$", message = DetailMessage.INVALID_PHONE_PATTERN)
	private String phone;

	public PhoneServiceRequest toServiceRequest() {

		return new PhoneServiceRequest(this.phone);
	}
}
