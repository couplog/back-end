package com.dateplan.dateplan.global.exception.schedule;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class InvalidRepeatEndTimeRange extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 4523544169010461820L;

	public InvalidRepeatEndTimeRange() {
		super(DetailMessage.INVALID_REPEAT_END_TIME_RANGE, ErrorCode.INVALID_REPEAT_END_TIME_RANGE);
	}

}
