package com.dateplan.dateplan.global.dto.response;

import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

	private boolean success;
	private T data;
	private String code;
	private String message;

	public static ApiResponse<Void> ofSuccess() {
		return new ApiResponse<>(true, null, null, null);
	}

	public static <T> ApiResponse<T> ofSuccess(T data) {
		return new ApiResponse<>(true, data, null, null);
	}

	public static ApiResponse<Void> ofFail(ApplicationException e) {
		return new ApiResponse<>(false, null, e.getErrorCode().getCode(), e.getMessage());
	}

	public static ApiResponse<Void> ofFail(ErrorCode errorCode, String message) {
		return new ApiResponse<>(false, null, errorCode.getCode(), message);
	}
}
