package com.dateplan.dateplan.global.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleDateUtil {

	public static LocalDate getNextMonthDate(int year, int month, int day) {
		do {
			month++;
			if (month > 12) {
				year++;
				month = 1;
			}
		} while(!isValidDate(year, month, day));
		return LocalDate.of(year, month, day);
	}

	public static LocalDate getNextYearDate(int year, int month, int day) {
		do {
			year++;
		} while(!isValidDate(year, month, day));
		return LocalDate.of(year, month, day);
	}

	private static boolean isValidDate(int year, int month, int day) {
		try {
			LocalDate.of(year, month, day);
			return true;
		} catch (DateTimeException e) {
			return false;
		}
	}
}
