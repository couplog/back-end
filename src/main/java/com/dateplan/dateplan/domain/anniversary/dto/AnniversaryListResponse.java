package com.dateplan.dateplan.domain.anniversary.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnniversaryListResponse {

	private List<AnniversaryResponse> anniversaries;

	public static AnniversaryListResponse from(AnniversaryListServiceResponse serviceResponse) {

		List<AnniversaryResponse> anniversaries = serviceResponse.getAnniversaries().stream()
			.map(AnniversaryResponse::from)
			.toList();

		return AnniversaryListResponse.builder()
			.anniversaries(anniversaries)
			.build();
	}
}
