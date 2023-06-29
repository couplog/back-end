package com.dateplan.dateplan.domain.anniversary.service.dto.request;

import static com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule.NONE;
import static com.dateplan.dateplan.global.constant.DateConstants.CALENDER_END_DATE;

import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AnniversaryCreateServiceRequest {

	private String title;
	private String content;
	private AnniversaryRepeatRule repeatRule;
	private LocalDate date;

	@Builder
	public AnniversaryCreateServiceRequest(String title, String content,
		AnniversaryRepeatRule repeatRule, LocalDate date) {
		this.title = title;
		this.content = content;
		this.repeatRule = repeatRule;
		this.date = date;
	}

	public AnniversaryPattern toAnniversaryPattern(Couple couple) {

		LocalDate repeatEndDate = repeatRule == NONE ? date : CALENDER_END_DATE;

		return AnniversaryPattern.builder()
			.couple(couple)
			.repeatStartDate(date)
			.repeatEndDate(repeatEndDate)
			.repeatRule(repeatRule)
			.build();
	}

}
