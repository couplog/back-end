package com.dateplan.dateplan.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedURLResponse {

	private String presignedURL;
}
