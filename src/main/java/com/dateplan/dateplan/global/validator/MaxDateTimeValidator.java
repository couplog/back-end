package com.dateplan.dateplan.global.validator;

import com.dateplan.dateplan.global.constant.DateConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class MaxDateTimeValidator implements ConstraintValidator<BeforeCalenderEndTime, LocalDateTime> {

	private String message;

	@Override
	public void initialize(BeforeCalenderEndTime constraintAnnotation) {
		ConstraintValidator.super.initialize(constraintAnnotation);
		this.message = constraintAnnotation.message();
	}

	@Override
	public boolean isValid(LocalDateTime time, ConstraintValidatorContext context) {

		if (time != null && DateConstants.CALENDER_END_DATE_TIME.isBefore(time)) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
			return false;
		}

		return true;
	}
}
