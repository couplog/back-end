package com.dateplan.dateplan.global.util;

import com.dateplan.dateplan.global.constant.RepeatRule;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleDateUtil {

	public static LocalDateTime getNextCycle(LocalDateTime now, RepeatRule rule) {
		if (rule.equals(RepeatRule.D)) {
			return getNextDayDate(now);
		}
		if (rule.equals(RepeatRule.W)) {
			return getNextWeekDate(now);
		}
		if (rule.equals(RepeatRule.M)) {
			return getNextMonthDate(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now);
		}
		return getNextYearDate(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now);
	}

	private static LocalDateTime getNextDayDate(LocalDateTime now) {
		return now.plusDays(1);
	}

	public static LocalDateTime getNextWeekDate(LocalDateTime now) {
		return now.plusDays(7);
	}

	private static LocalDateTime getNextMonthDate(int year, int month, int day, LocalDateTime now) {
		do {
			month++;
			if (month > 12) {
				year++;
				month = 1;
			}
		} while (!isValidDate(year, month, day, now));
		return LocalDateTime.of(year, month, day, now.getHour(), now.getMinute());
	}

	private static LocalDateTime getNextYearDate(int year, int month, int day, LocalDateTime now) {
		do {
			year++;
		} while (!isValidDate(year, month, day, now));
		return LocalDateTime.of(year, month, day, now.getHour(), now.getMinute());
	}

	private static boolean isValidDate(int year, int month, int day, LocalDateTime now) {
		try {
			LocalDateTime.of(year, month, day, now.getHour(), now.getMinute());
			return true;
		} catch (DateTimeException e) {
			return false;
		}
	}
}
