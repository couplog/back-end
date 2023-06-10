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
public class PhoneRequest {

	@NotNull(message = DetailMessage.INVALID_PHONE_PATTERN)
	@Pattern(regexp = "^010\\d{4}\\d{4}$", message = DetailMessage.INVALID_PHONE_PATTERN)
	private String phone;

	public PhoneServiceRequest toServiceRequest() {

		return new PhoneServiceRequest(this.phone);
	}
}
