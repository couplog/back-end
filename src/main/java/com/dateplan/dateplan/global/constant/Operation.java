package com.dateplan.dateplan.global.constant;

public enum Operation {

	CREATE("생성"),
	READ("조회"),
	UPDATE("수정"),
	DELETE("삭제");

	private final String name;

	Operation(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
