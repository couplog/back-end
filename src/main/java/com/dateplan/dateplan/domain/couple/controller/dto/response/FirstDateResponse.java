package com.dateplan.dateplan.domain.couple.controller.dto.response;

import com.dateplan.dateplan.domain.couple.service.dto.response.FirstDateServiceResponse;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FirstDateResponse {

	private LocalDate firstDate;

	public static FirstDateResponse from(FirstDateServiceResponse response) {
		return FirstDateResponse.builder()
			.firstDate(response.getFirstDate())
			.build();
	}
}
