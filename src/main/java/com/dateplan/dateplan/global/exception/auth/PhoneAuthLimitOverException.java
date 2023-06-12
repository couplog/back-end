package com.dateplan.dateplan.global.exception.auth;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class PhoneAuthLimitOverException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -314102075904828733L;

	public PhoneAuthLimitOverException() {
		super(DetailMessage.PHONE_AUTH_LIMIT_OVER, ErrorCode.PHONE_AUTH_LIMIT_OVER);
	}
}
