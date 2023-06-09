package com.dateplan.dateplan.domain.anniversary.controller.dto.response;

import com.dateplan.dateplan.domain.anniversary.service.dto.response.ComingAnniversaryListServiceResponse;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComingAnniversaryListResponse {

	private List<ComingAnniversaryResponse> anniversaries;

	public static ComingAnniversaryListResponse from(
		ComingAnniversaryListServiceResponse serviceResponse) {

		List<ComingAnniversaryResponse> responseList = serviceResponse.getAnniversaries()
			.stream()
			.map(ComingAnniversaryResponse::from)
			.toList();

		return ComingAnniversaryListResponse.builder()
			.anniversaries(responseList)
			.build();
	}
}
