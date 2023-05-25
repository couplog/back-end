package com.dateplan.dateplan.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthToken {

	private String accessToken;
	private String refreshToken;

}
