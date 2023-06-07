package com.dateplan.dateplan.domain.couple.dto;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public class FirstDateServiceResponse {

	private LocalDate firstDate;

	public FirstDateResponse toFirstDateResponse() {
		return FirstDateResponse.builder()
			.firstDate(firstDate)
			.build();
	}

}
