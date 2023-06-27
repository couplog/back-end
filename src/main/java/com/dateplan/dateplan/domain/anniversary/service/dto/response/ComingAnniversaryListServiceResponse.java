package com.dateplan.dateplan.domain.anniversary.service.dto.response;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ComingAnniversaryListServiceResponse {

	private List<ComingAnniversaryServiceResponse> anniversaries;

	public static ComingAnniversaryListServiceResponse from(List<Anniversary> anniversaries) {

		List<ComingAnniversaryServiceResponse> serviceResponseList = anniversaries.stream()
			.map(ComingAnniversaryServiceResponse::from)
			.toList();

		return ComingAnniversaryListServiceResponse.builder()
			.anniversaries(serviceResponseList)
			.build();
	}
}
