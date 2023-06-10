package com.dateplan.dateplan.global.exception.couple;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class MemberNotConnectedException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -8873935599350415525L;

	public MemberNotConnectedException() {
		super(DetailMessage.Member_NOT_CONNECTED, ErrorCode.MEMBER_NOT_CONNECTED);
	}
}
