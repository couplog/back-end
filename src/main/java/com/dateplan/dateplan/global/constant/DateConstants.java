package com.dateplan.dateplan.global.constant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateConstants {

	public static final LocalDate CALENDER_END_DATE = LocalDate.of(2049, 12, 31);
	public static final LocalDate NEXT_DAY_FROM_CALENDER_END_DATE = CALENDER_END_DATE.plusDays(1);
	public static final LocalDateTime CALENDER_END_DATE_TIME = LocalDateTime.of(2049, 12, 31, 23, 59);
}
