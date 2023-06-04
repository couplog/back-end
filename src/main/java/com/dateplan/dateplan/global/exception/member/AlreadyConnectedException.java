package com.dateplan.dateplan.global.exception.member;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class AlreadyConnectedException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 8893007792098750914L;

	public AlreadyConnectedException() {
		super(DetailMessage.ALREADY_CONNECTED, ErrorCode.ALREADY_CONNECTED);
	}

}
