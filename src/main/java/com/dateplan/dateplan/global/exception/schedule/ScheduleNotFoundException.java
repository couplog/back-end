package com.dateplan.dateplan.global.exception.schedule;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class ScheduleNotFoundException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 4462775479893526070L;

	public ScheduleNotFoundException() {
		super(DetailMessage.SCHEDULE_NOT_FOUND, ErrorCode.SCHEDULE_NOT_FOUND);
	}
}
