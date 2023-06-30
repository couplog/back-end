package com.dateplan.dateplan.domain.couple.controller.dto.response;

import com.dateplan.dateplan.domain.couple.service.dto.response.CoupleInfoServiceResponse;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CoupleInfoResponse {

	private Long coupleId;
	private Long partnerId;
	private LocalDate firstDate;

	public static CoupleInfoResponse from(CoupleInfoServiceResponse response) {
		return CoupleInfoResponse.builder()
			.coupleId(response.getCoupleId())
			.partnerId(response.getPartnerId())
			.firstDate(response.getFirstDate())
			.build();
	}
}
