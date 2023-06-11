package com.dateplan.dateplan.global.exception.auth;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class PhoneNotAuthenticatedException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 7793454994481070124L;

	public PhoneNotAuthenticatedException() {
		super(DetailMessage.NOT_AUTHENTICATED_PHONE, ErrorCode.NOT_AUTHENTICATED_PHONE);
	}
}
