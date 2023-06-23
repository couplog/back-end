package com.dateplan.dateplan.domain.anniversary.dto;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnniversaryListServiceResponse {

	private List<AnniversaryServiceResponse> anniversaries;

	public static AnniversaryListServiceResponse from(List<Anniversary> anniversaries) {

		List<AnniversaryServiceResponse> anniversaryServiceResponses = anniversaries.stream()
			.map(AnniversaryServiceResponse::of)
			.toList();

		return AnniversaryListServiceResponse.builder()
			.anniversaries(anniversaryServiceResponses)
			.build();
	}
}
