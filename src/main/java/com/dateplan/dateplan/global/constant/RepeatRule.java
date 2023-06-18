package com.dateplan.dateplan.global.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RepeatRule {

	N, D, W, M, Y;

	@JsonCreator
	public static RepeatRule from(String rule) {
		try {
			return RepeatRule.valueOf(rule.toUpperCase());
		} catch (NullPointerException | IllegalArgumentException e) {
			return null;
		}
	}
}
