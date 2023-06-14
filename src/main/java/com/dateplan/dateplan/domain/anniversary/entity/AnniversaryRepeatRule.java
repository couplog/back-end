package com.dateplan.dateplan.domain.anniversary.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Set;

public enum AnniversaryRepeatRule {

	NONE, HUNDRED_DAYS, YEAR;

	private static final Set<AnniversaryRepeatRule> POSSIBLE_INPUT_ENUMS = Set.of(NONE, YEAR);

	@JsonCreator
	public static AnniversaryRepeatRule from(String rule) {

		try {
			AnniversaryRepeatRule repeatRule = AnniversaryRepeatRule.valueOf(rule.toUpperCase());

			if (!POSSIBLE_INPUT_ENUMS.contains(repeatRule)) {
				repeatRule = null;
			}

			return repeatRule;

		} catch (NullPointerException | IllegalArgumentException e) {
			return null;
		}
	}
}
