package com.dateplan.dateplan.global.exception.couple;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class CoupleNotFoundException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -8065176825797390754L;

	public CoupleNotFoundException() {
		super(DetailMessage.COUPLE_NOT_FOUND, ErrorCode.COUPLE_NOT_FOUND);
	}
}
