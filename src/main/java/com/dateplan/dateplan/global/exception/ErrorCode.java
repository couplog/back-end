package com.dateplan.dateplan.global.exception;

import static org.springframework.http.HttpStatus.*;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

	// SERVER
	SERVER_ERROR(INTERNAL_SERVER_ERROR, "S001");

	private final HttpStatus httpStatusCode;
	private final String code;

	ErrorCode(HttpStatus httpStatusCode, String code) {
		this.httpStatusCode = httpStatusCode;
		this.code = code;
	}

	public static class DetailMessage{

		private DetailMessage(){}

		// SERVER
		public static final String SERVER_ERROR = "서버 내부에 문제가 생겼습니다.";
	}
}
