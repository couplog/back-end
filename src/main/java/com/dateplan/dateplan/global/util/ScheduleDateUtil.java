package com.dateplan.dateplan.global.util;

import com.dateplan.dateplan.global.constant.RepeatRule;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleDateUtil {

	public static LocalDateTime getNextCycle(LocalDateTime now, RepeatRule rule, int count) {
		if (rule.equals(RepeatRule.D)) {
			return getNextDayDate(now, count);
		}
		if (rule.equals(RepeatRule.W)) {
			return getNextWeekDate(now, count);
		}
		if (rule.equals(RepeatRule.M)) {
			return getNextMonthDate(now, count);
		}
		return getNextYearDate(now, count);
	}

	private static LocalDateTime getNextDayDate(LocalDateTime now, int count) {
		return now.plusDays(count);
	}

	public static LocalDateTime getNextWeekDate(LocalDateTime now, int count) {
		return now.plusWeeks(count);
	}

	private static LocalDateTime getNextMonthDate(LocalDateTime now, int count) {
		return now.plusMonths(count);
	}

	private static LocalDateTime getNextYearDate(LocalDateTime now, int count) {
		return now.plusYears(count);
	}
}
