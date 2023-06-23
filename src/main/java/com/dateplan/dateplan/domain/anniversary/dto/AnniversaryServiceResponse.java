package com.dateplan.dateplan.domain.anniversary.dto;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnniversaryServiceResponse {

	private Long id;
	private String title;
	private String content;
	private AnniversaryRepeatRule repeatRule;
	private LocalDate date;

	public static AnniversaryServiceResponse of(Anniversary anniversary) {

		return AnniversaryServiceResponse.builder()
			.id(anniversary.getId())
			.title(anniversary.getTitle())
			.content(anniversary.getContent())
			.repeatRule(anniversary.getAnniversaryPattern().getRepeatRule())
			.date(anniversary.getDate())
			.build();
	}
}
