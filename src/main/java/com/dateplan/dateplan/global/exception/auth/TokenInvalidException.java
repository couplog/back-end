package com.dateplan.dateplan.global.exception.auth;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class TokenInvalidException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -285169637497395188L;

	public TokenInvalidException() {
		super(DetailMessage.TOKEN_INVALID, ErrorCode.TOKEN_INVALID);
	}

}
