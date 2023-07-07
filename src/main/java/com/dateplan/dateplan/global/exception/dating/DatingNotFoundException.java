package com.dateplan.dateplan.global.exception.dating;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class DatingNotFoundException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 6295191075811653477L;

	public DatingNotFoundException() {
		super(DetailMessage.DATING_NOT_FOUND, ErrorCode.DATING_NOT_FOUND);
	}
}