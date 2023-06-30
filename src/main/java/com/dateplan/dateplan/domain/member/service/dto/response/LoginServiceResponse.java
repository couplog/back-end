package com.dateplan.dateplan.domain.member.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginServiceResponse {

	private AuthToken authToken;
	private Boolean isConnected;
}
