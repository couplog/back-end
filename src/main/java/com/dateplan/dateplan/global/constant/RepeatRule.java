package com.dateplan.dateplan.global.constant;

public enum RepeatRule {

	N("없음"),
	D("매일"),
	W("매주"),
	M("매월"),
	Y("매년");

	private final String rule;

	RepeatRule(final String rule) {
		this.rule = rule;
	}

	public String getRule() {
		return rule;
	}
}
