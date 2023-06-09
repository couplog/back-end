package com.dateplan.dateplan.global.exception.handler;

import com.dateplan.dateplan.global.dto.response.ApiResponse;
import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> methodArgumentNotValidException(
		MethodArgumentNotValidException e) {

		ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
		String message = e.getAllErrors().get(0).getDefaultMessage();

		ApiResponse<Void> response = ApiResponse.ofFail(errorCode, message);

		return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiResponse<Void>> noHandlerFoundException(NoHandlerFoundException e) {

		ErrorCode errorCode = ErrorCode.URL_NOT_FOUND;
		String message = String.format(DetailMessage.URL_NOT_FOUND, e.getRequestURL());

		ApiResponse<Void> response = ApiResponse.ofFail(ErrorCode.URL_NOT_FOUND, message);

		return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> methodNotAllowedException(
		HttpRequestMethodNotSupportedException e) {

		ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
		String message = String.format(DetailMessage.METHOD_NOT_ALLOWED, e.getMethod(),
			Arrays.toString(e.getSupportedMethods()));

		ApiResponse<Void> response = ApiResponse.ofFail(errorCode, message);

		return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> methodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException e) {

		ErrorCode errorCode = ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH;
		String message = String.format(DetailMessage.METHOD_ARGUMENT_TYPE_MISMATCH,
			e.getPropertyName(), e.getRequiredType().getSimpleName());

		ApiResponse<Void> response = ApiResponse.ofFail(errorCode, message);

		return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> missingServletRequestParameterException(
		MissingServletRequestParameterException e) {

		ErrorCode errorCode = ErrorCode.MISSING_REQUEST_PARAMETER;
		String message = String.format(DetailMessage.MISSING_REQUEST_PARAMETER,
			e.getParameterName());

		ApiResponse<Void> response = ApiResponse.ofFail(errorCode, message);

		return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> httpMediaTypeNotSupportedException(
		HttpMediaTypeNotSupportedException e) {

		ErrorCode errorCode = ErrorCode.MEDIA_TYPE_NOT_SUPPORTED;
		String message = String.format(DetailMessage.MEDIA_TYPE_NOT_SUPPORTED, e.getContentType(),
			e.getSupportedMediaTypes());

		ApiResponse<Void> response = ApiResponse.ofFail(errorCode, message);

		return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
	}

	@ExceptionHandler(ApplicationException.class)
	public ResponseEntity<ApiResponse<Void>> handleRuntimeException(ApplicationException e) {

		ErrorCode errorCode = e.getErrorCode();
		String message = e.getMessage();
		ApiResponse<Void> response = ApiResponse.ofFail(errorCode, message);

		return ResponseEntity.status(e.getErrorCode().getHttpStatusCode()).body(response);
	}

	@ExceptionHandler(DateTimeParseException.class)
	public ResponseEntity<ApiResponse<Void>> handleDateTimeParseException(DateTimeParseException e) {

		ApiResponse<Void> response = ApiResponse.ofFail(ErrorCode.INVALID_INPUT_VALUE, DetailMessage.INVALID_DATE_PATTERN);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
}
