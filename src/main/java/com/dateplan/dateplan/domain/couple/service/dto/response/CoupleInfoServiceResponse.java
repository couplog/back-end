package com.dateplan.dateplan.domain.couple.service.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CoupleInfoServiceResponse {

	private Long coupleId;
	private Long partnerId;
	private LocalDate firstDate;
}
