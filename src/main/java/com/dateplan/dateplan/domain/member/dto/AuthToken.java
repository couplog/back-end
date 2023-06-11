package com.dateplan.dateplan.domain.member.dto;

import static com.dateplan.dateplan.global.constant.Auth.*;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthToken {

	private String accessToken;
	private String refreshToken;

	public String getAccessTokenWithoutPrefix() {
		return accessToken.replaceAll(BEARER.getContent(), "");
	}

	public String getRefreshTokenWithoutPrefix() {
		return refreshToken.replaceAll(BEARER.getContent(), "");
	}
}