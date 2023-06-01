package com.dateplan.dateplan.global.exception;

import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class AlReadyRegisteredNicknameException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -3264682654466649985L;

	public AlReadyRegisteredNicknameException() {
		super(DetailMessage.ALREADY_REGISTERED_NICKNAME, ErrorCode.ALREADY_REGISTERED_NICKNAME);
	}
}
