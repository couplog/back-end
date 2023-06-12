package com.dateplan.dateplan.domain.member.dto.signup;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SendSmsServiceResponse {

	private Integer currentCount;

	public SendSmsResponse toResponse() {
		return SendSmsResponse.builder()
			.currentCount(this.currentCount)
			.build();
	}
}
