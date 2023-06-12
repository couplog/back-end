package com.dateplan.dateplan.global.exception.auth;

import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.io.Serial;

public class NoPermissionException extends ApplicationException {

	@Serial
	private static final long serialVersionUID = -892719849005433343L;

	public NoPermissionException(Resource resource, Operation operation) {

		super(String.format(DetailMessage.NO_PERMISSION, resource.getName(), operation.getName())
			, ErrorCode.NO_PERMISSION);
	}
}
