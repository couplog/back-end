package com.dateplan.dateplan.global.validator;

import com.dateplan.dateplan.global.constant.DateConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class MaxDateTimeValidator implements ConstraintValidator<BeforeCalenderEndTime, LocalDate> {

	private String message;

	@Override
	public void initialize(BeforeCalenderEndTime constraintAnnotation) {
		ConstraintValidator.super.initialize(constraintAnnotation);
		this.message = constraintAnnotation.message();
	}

	@Override
	public boolean isValid(LocalDate time, ConstraintValidatorContext context) {

		if (DateConstants.CALENDER_END_DATE.isBefore(time)) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
			return false;
		}

		return true;
	}
}
