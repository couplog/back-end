package com.dateplan.dateplan.global.exception;

import java.io.Serial;

public class S3Exception extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -7201762512260194006L;

	public S3Exception(String message, Throwable t) {
		super(message, ErrorCode.S3_ERROR, t);
	}
}
