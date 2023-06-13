package com.dateplan.dateplan.global.exception.auth;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class InvalidPhoneAuthCodeException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -5479970169829209800L;

	public InvalidPhoneAuthCodeException(String code) {
		super(code == null ? DetailMessage.PHONE_AUTH_CODE_NOT_EXISTS
			: DetailMessage.PHONE_AUTH_CODE_NOT_MATCH, ErrorCode.INVALID_PHONE_AUTH_CODE);
	}
}
