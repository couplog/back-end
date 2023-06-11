package com.dateplan.dateplan.global.constant;

public enum Resource {

	MEMBER("회원"),
	COUPLE("커플");

	private final String name;

	Resource(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
