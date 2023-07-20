package com.dateplan.dateplan.global.exception.auth;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class TokenNotFoundException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 4429870598490380089L;

	public TokenNotFoundException(String a) {
		super(DetailMessage.TOKEN_NOT_FOUND + " " + a, ErrorCode.TOKEN_NOT_FOUND);
	}

}
