package com.dateplan.dateplan.global.exception.sms;

import com.dateplan.dateplan.domain.sms.type.SmsType;
import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class SmsSendFailException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 500434495823959635L;

	public SmsSendFailException(SmsType type) {
		super(String.format(DetailMessage.SMS_SEND_FAIL, type.getName()), ErrorCode.SMS_SEND_FAIL);
	}
}
