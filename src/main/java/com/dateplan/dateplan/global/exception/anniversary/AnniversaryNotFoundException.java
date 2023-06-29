package com.dateplan.dateplan.global.exception.anniversary;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class AnniversaryNotFoundException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -963976559194270062L;

	public AnniversaryNotFoundException() {
		super(DetailMessage.ANNIVERSARY_NOT_FOUND, ErrorCode.ANNIVERSARY_NOT_FOUND);
	}
}
