package com.dateplan.dateplan.domain.member.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedURLResponse {

	private String presignedURL;
}
