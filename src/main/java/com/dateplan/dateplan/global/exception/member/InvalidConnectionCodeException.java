package com.dateplan.dateplan.global.exception.member;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class InvalidConnectionCodeException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 4024595526063695975L;

	public InvalidConnectionCodeException() {
		super(DetailMessage.INVALID_CONNECTION_CODE, ErrorCode.INVALID_CONNECTION_CODE);
	}

}
