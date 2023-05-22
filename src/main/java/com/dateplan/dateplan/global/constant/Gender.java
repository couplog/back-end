package com.dateplan.dateplan.global.constant;

public enum Gender {

	MALE("남"),
	FEMALE("여");

	private final String gender;

	Gender(String gender) {
		this.gender = gender;
	}

	public String getGender() {
		return gender;
	}
}
