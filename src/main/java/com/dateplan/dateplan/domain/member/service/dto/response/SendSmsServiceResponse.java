package com.dateplan.dateplan.domain.member.service.dto.response;

import com.dateplan.dateplan.domain.member.controller.dto.response.SendSmsResponse;
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
