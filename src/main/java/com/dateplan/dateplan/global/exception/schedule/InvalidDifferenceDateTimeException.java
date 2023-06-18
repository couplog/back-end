package com.dateplan.dateplan.global.exception.schedule;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class InvalidDifferenceDateTimeException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -3785340240555744153L;

	public InvalidDifferenceDateTimeException() {
		super(DetailMessage.INVALID_DIFFERENCE_DATE_TIME, ErrorCode.INVALID_DIFFERENCE_DATE_TIME);
	}
}
