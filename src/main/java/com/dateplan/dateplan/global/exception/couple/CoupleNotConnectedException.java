package com.dateplan.dateplan.global.exception.couple;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class CoupleNotConnectedException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -8873935599350415525L;

	public CoupleNotConnectedException() {
		super(DetailMessage.COUPLE_NOT_CONNECTED, ErrorCode.COUPLE_NOT_CONNECTED);
	}
}
