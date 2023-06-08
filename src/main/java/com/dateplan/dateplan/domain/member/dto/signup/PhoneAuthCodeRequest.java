package com.dateplan.dateplan.domain.member.dto.signup;

import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PhoneAuthCodeRequest {

	@NotNull(message = DetailMessage.INVALID_PHONE_PATTERN)
	@Pattern(regexp = "^010\\d{4}\\d{4}$", message = DetailMessage.INVALID_PHONE_PATTERN)
	private String phone;

	@NotNull(message = DetailMessage.INVALID_PHONE_AUTH_CODE_PATTERN)
	@Pattern(regexp = "^\\d{6}$", message = DetailMessage.INVALID_PHONE_AUTH_CODE_PATTERN)
	private String code;

	public PhoneAuthCodeServiceRequest toServiceRequest() {
		return new PhoneAuthCodeServiceRequest(this.phone, this.code);
	}
}
