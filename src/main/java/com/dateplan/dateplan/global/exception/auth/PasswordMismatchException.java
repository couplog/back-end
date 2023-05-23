package com.dateplan.dateplan.global.exception.auth;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class PasswordMismatchException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -9077922712668461975L;

	public PasswordMismatchException() {
		super(DetailMessage.PASSWORD_MISMATCH, ErrorCode.PASSWORD_MISMATCH);
	}

}
