package com.dateplan.dateplan.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConnectionServiceResponse {

	private String connectionCode;

	public ConnectionResponse toConnectionResponse() {
		return ConnectionResponse.builder()
			.connectionCode(connectionCode)
			.build();
	}
}
