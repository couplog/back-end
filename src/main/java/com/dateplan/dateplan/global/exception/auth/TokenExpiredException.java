package com.dateplan.dateplan.global.exception.auth;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class TokenExpiredException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 7231517269055889719L;

	public TokenExpiredException() {
		super(DetailMessage.TOKEN_INVALID, ErrorCode.TOKEN_INVALID);
	}

}
