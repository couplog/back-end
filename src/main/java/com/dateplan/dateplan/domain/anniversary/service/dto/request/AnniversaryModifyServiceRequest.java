package com.dateplan.dateplan.domain.anniversary.service.dto.request;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnniversaryModifyServiceRequest {

	private String title;
	private String content;
	private LocalDate date;
}
