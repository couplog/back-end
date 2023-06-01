package com.dateplan.dateplan.global.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

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

	@JsonCreator
	public static Gender from(String genderStr) {
		try {
			return Gender.valueOf(genderStr.toUpperCase());
		} catch (NullPointerException | IllegalArgumentException e) {
			return null;
		}
	}
}
