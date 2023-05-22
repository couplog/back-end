package com.dateplan.dateplan.global.exception;

import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class AlReadyRegisteredPhoneException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 1736621801582995841L;

	public AlReadyRegisteredPhoneException() {
		super(DetailMessage.ALREADY_REGISTERED_PHONE, ErrorCode.ALREADY_REGISTERED_PHONE);
	}
}
