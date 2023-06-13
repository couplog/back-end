package com.dateplan.dateplan.global.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RepeatRule {

	N("없음"),
	D("매일"),
	W("매주"),
	M("매월"),
	Y("매년");

	private final String rule;

	RepeatRule(String rule) {
		this.rule = rule;
	}

	public String getRule() {
		return rule;
	}

	@JsonCreator
	public static RepeatRule from(String rule) {
		try {
			return RepeatRule.valueOf(rule.toUpperCase());
		} catch (NullPointerException | IllegalArgumentException e) {
			return null;
		}
	}
}
