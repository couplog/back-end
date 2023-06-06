package com.dateplan.dateplan.global.exception;

import java.io.Serial;
import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -8808014635391651324L;

	private final ErrorCode errorCode;

	public ApplicationException(String message, ErrorCode errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public ApplicationException(String message, ErrorCode errorCode, Throwable t) {
		super(message, t);
		this.errorCode = errorCode;
	}
}
