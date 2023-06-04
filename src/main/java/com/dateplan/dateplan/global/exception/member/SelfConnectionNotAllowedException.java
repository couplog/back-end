package com.dateplan.dateplan.global.exception.member;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class SelfConnectionNotAllowedException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 7097469883905338039L;

	public SelfConnectionNotAllowedException() {
		super(DetailMessage.SELF_CONNECTION_NOT_ALLOWED, ErrorCode.SELF_CONNECTION_NOT_ALLOWED);
	}

}
