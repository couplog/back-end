package com.dateplan.dateplan.global.exception.schedule;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class InvalidDateTimeRangeException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -379141334883057132L;

	public InvalidDateTimeRangeException() {
		super(DetailMessage.INVALID_DATE_TIME_RANGE, ErrorCode.INVALID_DATE_TIME_RANGE);
	}

}
