package com.dateplan.dateplan.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileImageURLServiceResponse {

	private String profileImageURL;

	public ProfileImageURLResponse toResponse() {

		return ProfileImageURLResponse.builder()
			.profileImageURL(this.profileImageURL)
			.build();
	}
}
