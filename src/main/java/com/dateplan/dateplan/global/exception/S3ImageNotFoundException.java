package com.dateplan.dateplan.global.exception;

import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class S3ImageNotFoundException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = 7732979753217622829L;

	public S3ImageNotFoundException() {
		super(DetailMessage.S3_IMAGE_NOT_FOUND, ErrorCode.S3_IMAGE_NOT_FOUND);
	}
}
