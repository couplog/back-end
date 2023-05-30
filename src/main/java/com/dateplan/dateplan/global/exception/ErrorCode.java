package com.dateplan.dateplan.global.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

	// CLIENT
	URL_NOT_FOUND(HttpStatus.NOT_FOUND, "C001"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002"),
	METHOD_ARGUMENT_TYPE_MISMATCH(BAD_REQUEST, "C003"),
	MISSING_REQUEST_PARAMETER(BAD_REQUEST, "C004"),
	MEDIA_TYPE_NOT_SUPPORTED(UNSUPPORTED_MEDIA_TYPE, "C005"),
	MEMBER_NOT_FOUND(BAD_REQUEST, "C006"),
	TOKEN_EXPIRED(UNAUTHORIZED, "C007"),
	TOKEN_INVALID(UNAUTHORIZED, "C008"),
	PASSWORD_MISMATCH(UNAUTHORIZED, "C009"),
	METHOD_ARGUMENT_INVALID(BAD_REQUEST, "C010"),
	ALREADY_REGISTERED_PHONE(CONFLICT, "C011"),
	INVALID_INPUT_VALUE(BAD_REQUEST, "C012"),
	INVALID_PHONE_AUTH_CODE(CONFLICT, "C013"),

	// SERVER
	SERVER_ERROR(INTERNAL_SERVER_ERROR, "S001"),
	SMS_SEND_FAIL(SERVICE_UNAVAILABLE, "S002");

	private final HttpStatus httpStatusCode;
	private final String code;

	ErrorCode(HttpStatus httpStatusCode, String code) {
		this.httpStatusCode = httpStatusCode;
		this.code = code;
	}

	public static class DetailMessage {

		private DetailMessage() {
		}

		// CLIENT
		public static final String URL_NOT_FOUND = "%s : 해당 경로는 존재하지 않는 경로입니다.";
		public static final String METHOD_NOT_ALLOWED = "%s : 해당 HTTP 메소드는 지원되지 않습니다. 허용 메소드 : %s";
		public static final String METHOD_ARGUMENT_TYPE_MISMATCH = "요청 파라미터에서 %s 값은 %s 타입이어야 합니다.";
		public static final String MISSING_REQUEST_PARAMETER = "요청 파라미터에서 %s 값은 필수입니다.";
		public static final String MEDIA_TYPE_NOT_SUPPORTED = "%s : 지원하지 않는 media type 입니다. 지원 type : %s";
		public static final String ALREADY_REGISTERED_PHONE = "이미 가입된 전화번호입니다.";
		public static final String INVALID_PHONE_PATTERN = "전화번호는 010AAAABBBB 형식으로 입력해 주세요";
		public static final String INVALID_PHONE_AUTH_CODE_PATTERN = "전화번호 인증코드는 6자리 숫자입니다.";
		public static final String PHONE_AUTH_CODE_NOT_EXISTS = "전화번호 인증코드가 존재하지 않습니다. 다시 인증해 주세요.";
		public static final String PHONE_AUTH_CODE_NOT_MATCH = "전화번호 인증코드가 일치하지 않습니다.";
		public static final String MEMBER_NOT_FOUND = "유저가 존재하지 않습니다.";
		public static final String TOKEN_EXPIRED = "토큰이 만료되었습니다.";
		public static final String TOKEN_INVALID = "유효하지 않은 토큰입니다.";
		public static final String PASSWORD_MISMATCH = "비밀번호가 올바르지 않습니다.";

		// SERVER
		public static final String SERVER_ERROR = "서버 내부에 문제가 생겼습니다.";
		public static final String SMS_SEND_FAIL = "%s 문자를 전송하던 중 문제가 발생하였습니다. 잠시 후에 다시 시도해 주세요.";
	}
}
