package com.dateplan.dateplan.domain.anniversary.service.dto.response;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryCategory;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
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
	private AnniversaryCategory category;
	private LocalDate date;

	public static AnniversaryServiceResponse of(Anniversary anniversary) {

		AnniversaryPattern anniversaryPattern = anniversary.getAnniversaryPattern();

		return AnniversaryServiceResponse.builder()
			.id(anniversary.getId())
			.title(anniversary.getTitle())
			.content(anniversary.getContent())
			.repeatRule(anniversaryPattern.getRepeatRule())
			.category(anniversaryPattern.getCategory())
			.date(anniversary.getDate())
			.build();
	}
}
