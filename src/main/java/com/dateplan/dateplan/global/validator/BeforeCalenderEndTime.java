package com.dateplan.dateplan.global.validator;

import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import jakarta.validation.Constraint;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = {MaxDateValidator.class, MaxDateTimeValidator.class})
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeforeCalenderEndTime {

	String message() default DetailMessage.INVALID_CALENDER_TIME_RANGE;

	Class[] groups() default {};

	Class[] payload() default {};
}
