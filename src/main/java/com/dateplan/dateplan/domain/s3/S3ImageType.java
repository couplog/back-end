package com.dateplan.dateplan.domain.s3;

public enum S3ImageType {

	MEMBER_PROFILE("회원 프로필", "members/profile/");

	private final String type;
	private final String savedPath;

	S3ImageType(String type, String savedPath) {
		this.type = type;
		this.savedPath = savedPath;
	}

	public String getFullPath(String fileName){

		return this.savedPath + fileName;
	}
}
