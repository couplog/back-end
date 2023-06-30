package com.dateplan.dateplan.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConnectionResponse {

	private String connectionCode;

	public static ConnectionResponse from(ConnectionServiceResponse response) {
		return ConnectionResponse.builder()
			.connectionCode(response.getConnectionCode())
			.build();
	}
}
