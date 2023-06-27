package com.dateplan.dateplan.domain.anniversary.service.dto.response;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComingAnniversaryServiceResponse {

	private Long id;
	private String title;
	private String content;
	private LocalDate date;

	public static ComingAnniversaryServiceResponse from(Anniversary anniversary) {

		return ComingAnniversaryServiceResponse.builder()
			.id(anniversary.getId())
			.title(anniversary.getTitle())
			.content(anniversary.getContent())
			.date(anniversary.getDate())
			.build();
	}
}
