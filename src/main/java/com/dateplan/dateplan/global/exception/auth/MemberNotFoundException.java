package com.dateplan.dateplan.global.exception.auth;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class MemberNotFoundException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -8134332786210343088L;

	public MemberNotFoundException() {
		super(DetailMessage.MEMBER_NOT_FOUND, ErrorCode.MEMBER_NOT_FOUND);
	}
}
