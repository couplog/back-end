package com.dateplan.dateplan.domain.dating.service.dto.response;

import com.dateplan.dateplan.domain.dating.controller.dto.response.DatingEntry;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DatingServiceResponse {

	private List<DatingEntry> datingList;

	public static DatingServiceResponse from(List<Dating> datingList) {
		return DatingServiceResponse.builder()
			.datingList(datingList.stream()
				.map(DatingEntry::from)
				.toList())
			.build();
	}
}
