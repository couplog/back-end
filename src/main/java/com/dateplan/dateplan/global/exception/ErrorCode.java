package com.dateplan.dateplan.global.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
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
	ALREADY_REGISTERED_NICKNAME(CONFLICT, "C014"),
	NOT_AUTHENTICATED_PHONE(CONFLICT, "C015"),
	INVALID_CONNECTION_CODE(BAD_REQUEST, "C016"),
	ALREADY_CONNECTED(CONFLICT, "C017"),
	SELF_CONNECTION_NOT_ALLOWED(BAD_REQUEST, "C018"),
	S3_IMAGE_NOT_FOUND(CONFLICT, "C019"),
	TOKEN_NOT_FOUND(UNAUTHORIZED, "C020"),
	NO_PERMISSION(FORBIDDEN, "C021"),
	MEMBER_NOT_CONNECTED(BAD_REQUEST, "C022"),
	PHONE_AUTH_LIMIT_OVER(TOO_MANY_REQUESTS, "C023"),
	INVALID_DATE_TIME_RANGE(BAD_REQUEST, "C024"),
	INVALID_REPEAT_END_TIME_RANGE(BAD_REQUEST, "C025"),
	INVALID_DIFFERENCE_DATE_TIME(BAD_REQUEST, "C026"),
	COUPLE_NOT_FOUND(NOT_FOUND, "C027"),

	// SERVER
	SERVER_ERROR(INTERNAL_SERVER_ERROR, "S001"),
	SMS_SEND_FAIL(SERVICE_UNAVAILABLE, "S002"),
	S3_ERROR(SERVICE_UNAVAILABLE, "S003");

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

		// 공통
		public static final String URL_NOT_FOUND = "%s : 해당 경로는 존재하지 않는 경로입니다.";
		public static final String METHOD_NOT_ALLOWED = "%s : 해당 HTTP 메소드는 지원되지 않습니다. 허용 메소드 : %s";
		public static final String METHOD_ARGUMENT_TYPE_MISMATCH = "요청 파라미터에서 %s 값은 %s 타입이어야 합니다.";
		public static final String MISSING_REQUEST_PARAMETER = "요청 파라미터에서 %s 값은 필수입니다.";
		public static final String MEDIA_TYPE_NOT_SUPPORTED = "%s : 지원하지 않는 media type 입니다. 지원 type : %s";
		public static final String INVALID_DATE_PATTERN = "날짜는 yyyy-MM-dd, 시간은 yyyy-MM-ddTHH:mm 형식으로 입력해 주세요.";
		public static final String INVALID_CALENDER_TIME_RANGE = "날짜 및 시간은 2050 년 이전으로 입력해 주세요.";


		// 회원 관련
		public static final String ALREADY_REGISTERED_PHONE = "이미 가입된 전화번호입니다.";
		public static final String ALREADY_REGISTERED_NICKNAME = "이미 등록된 닉네임입니다.";

		public static final String INVALID_PHONE_PATTERN = "전화번호는 010AAAABBBB 형식으로 입력해 주세요.";
		public static final String INVALID_PHONE_AUTH_CODE_PATTERN = "전화번호 인증코드는 6자리 숫자입니다.";
		public static final String PHONE_AUTH_CODE_NOT_EXISTS = "전화번호 인증코드가 존재하지 않습니다. 다시 인증해 주세요.";
		public static final String PHONE_AUTH_CODE_NOT_MATCH = "전화번호 인증코드가 일치하지 않습니다.";
		public static final String NOT_AUTHENTICATED_PHONE = "인증되지 않은 전화번호 입니다. 전화번호 인증을 먼저 진행해 주세요.";
		public static final String MEMBER_NOT_FOUND = "유저가 존재하지 않습니다.";
		public static final String INVALID_MEMBER_NAME_PATTERN = "이름은 2-10 자의 한글로 입력해 주세요.";
		public static final String INVALID_NICKNAME_PATTERN = "닉네임은 2-10 자의 한글, 영문, 숫자로 입력해 주세요.";
		public static final String INVALID_PASSWORD_PATTERN = "비밀번호는 5-20 자의 영문, 숫자로 입력해 주세요.";
		public static final String INVALID_GENDER = "성별은 필수 값이며, male, female 중 하나로 입력해 주세요.";
		public static final String INVALID_BIRTH_RANGE = "생일은 현재 시간 이전의 범위에서 입력해 주세요.";
		public static final String INVALID_CONNECTION_CODE_PATTERN = "연결 코드는 6자리의 숫자와 영문 대문자 입니다.";
		public static final String INVALID_FIRST_DATE_RANGE = "처음 만난 날은 현재 시간 이전의 범위에서 입력해 주세요.";
		public static final String INVALID_CONNECTION_CODE = "존재하지 않는 연결 코드입니다.";
		public static final String ALREADY_CONNECTED = "이미 상대방이 연결되어 있습니다.";
		public static final String SELF_CONNECTION_NOT_ALLOWED = "자기 자신과 연결할 수 없습니다.";
		public static final String S3_IMAGE_NOT_FOUND = "S3 에 이미지가 존재하지 않습니다.";

		// 일정 관련
		public static final String INVALID_SCHEDULE_TITLE = "제목은 필수이며, 15자 이하여야 합니다.";
		public static final String INVALID_SCHEDULE_LOCATION = "일정 위치는 20자 이하여야 합니다.";
		public static final String INVALID_SCHEDULE_CONTENT = "일정 내용은 100자 이하여야 합니다.";
		public static final String INVALID_REPEAT_RULE = "일정 규칙은 'N', 'D', 'W', 'M', 'Y' 중 하나여야 합니다.";
		public static final String INVALID_SCHEDULE_TIME = "일정 시작 시간과 종료 시간은 필수입니다.";
		public static final String INVALID_DATE_TIME_RANGE = "일정 시작 시간은 일정 종료 시간 이전이어야 합니다.";
		public static final String INVALID_REPEAT_END_TIME_RANGE = "일정 반복 종료 시간은 2050년 이전이며, 일정 종료 시간 이후여야 합니다.";
		public static final String INVALID_DIFFERENCE_DATE_TIME = "일정 시작과 끝 사이의 간격은 반복 규칙보다 클 수 없습니다.";

		// 커플 관련
		public static final String Member_NOT_CONNECTED = "다른 회원과 아직 연결되지 않은 회원입니다.";
		public static final String COUPLE_NOT_FOUND = "존재하지 않는 커플입니다.";

		// 인증, 인가 관련
		public static final String TOKEN_EXPIRED = "토큰이 만료되었습니다.";
		public static final String TOKEN_INVALID = "유효하지 않은 토큰입니다.";
		public static final String PASSWORD_MISMATCH = "비밀번호가 올바르지 않습니다.";
		public static final String TOKEN_NOT_FOUND = "access token을 찾을 수 없습니다.";
		public static final String NO_PERMISSION = "해당 %s 에 대한 %s 권한이 없습니다.";
		public static final String PHONE_AUTH_LIMIT_OVER = "하루 전화번호 인증 횟수(5회)를 초과하였습니다. 24시간 이후에 다시 시도해 주세요.";

		// 기념일 관련
		public static final String INVALID_ANNIVERSARY_TITLE = "기념일 제목은 2 - 15자로 입력해 주세요.";
		public static final String INVALID_ANNIVERSARY_CONTENT = "기념일 내용은 100자 이하로 입력해 주세요.";
		public static final String INVALID_ANNIVERSARY_REPEAT_RULE = "반복 규칙은 NONE, YEAR 로 입력해 주세요.";

		// SERVER
		public static final String SERVER_ERROR = "서버 내부에 문제가 생겼습니다.";
		public static final String SMS_SEND_FAIL = "%s 문자를 전송하던 중 문제가 발생하였습니다. 잠시 후에 다시 시도해 주세요.";
		public static final String S3_CREATE_PRESIGNED_URL_FAIL = "Presigned URL 을 생성하던 중 문제가 발생하였습니다. 잠시 후에 다시 시도해 주세요.";
		public static final String S3_DELETE_OBJECT_FAIL = "S3 내의 객체를 삭제하던 중 문제가 발생하였습니다. 잠시 후에 다시 시도해 주세요";
	}
}
