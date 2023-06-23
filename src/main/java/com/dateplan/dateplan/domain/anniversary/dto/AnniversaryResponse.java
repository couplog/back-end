package com.dateplan.dateplan.domain.anniversary.dto;

import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnniversaryResponse {

	private Long id;
	private String title;
	private String content;
	private AnniversaryRepeatRule repeatRule;
	private LocalDate date;

	public static AnniversaryResponse from(AnniversaryServiceResponse serviceResponse){

		return AnniversaryResponse.builder()
			.id(serviceResponse.getId())
			.title(serviceResponse.getTitle())
			.content(serviceResponse.getContent())
			.repeatRule(serviceResponse.getRepeatRule())
			.date(serviceResponse.getDate())
			.build();
	}
}
