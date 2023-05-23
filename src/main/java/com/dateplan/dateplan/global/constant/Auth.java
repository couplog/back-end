package com.dateplan.dateplan.global.constant;

public enum Auth {

	// 2시간
	ACCESS_TOKEN_EXPIRATION(1_000L * 60 * 60 * 2),
	// 2주
	REFRESH_TOKEN_EXPIRATION(1_000L * 60 * 60 * 24 * 14),

	HEADER_AUTHORIZATION("Authorization"),
	HEADER_REFRESH_TOKEN("refreshToken"),

	SUBJECT_ACCESS_TOKEN("accessToken"),
	SUBJECT_REFRESH_TOKEN("refreshToken");

	private Long expiration;
	private String content;

	Auth(String content) {
		this.content = content;
	}

	Auth(Long expiration) {
		this.expiration = expiration;
	}

	public Long getExpiration() {
		return expiration;
	}

	public String getContent() {
		return content;
	}
}
