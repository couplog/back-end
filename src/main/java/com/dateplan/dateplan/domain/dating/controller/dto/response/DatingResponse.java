package com.dateplan.dateplan.domain.dating.controller.dto.response;

import com.dateplan.dateplan.domain.dating.service.dto.response.DatingServiceResponse;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DatingResponse {

	List<DatingEntry> datingList;

	public static DatingResponse from(DatingServiceResponse response) {
		return DatingResponse.builder()
			.datingList(response.getDatingList())
			.build();
	}
}
