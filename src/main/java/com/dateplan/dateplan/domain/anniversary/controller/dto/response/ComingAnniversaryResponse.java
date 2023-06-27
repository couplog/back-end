package com.dateplan.dateplan.domain.anniversary.controller.dto.response;

import com.dateplan.dateplan.domain.anniversary.service.dto.response.ComingAnniversaryServiceResponse;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComingAnniversaryResponse {

	private Long id;
	private String title;
	private String content;
	private LocalDate date;

	public static ComingAnniversaryResponse from(ComingAnniversaryServiceResponse serviceResponse){

		return ComingAnniversaryResponse.builder()
			.id(serviceResponse.getId())
			.title(serviceResponse.getTitle())
			.content(serviceResponse.getContent())
			.date(serviceResponse.getDate())
			.build();
	}
}
