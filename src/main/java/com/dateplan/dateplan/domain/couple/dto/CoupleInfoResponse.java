package com.dateplan.dateplan.domain.couple.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CoupleInfoResponse {

	private Long coupleId;
	private Long opponentId;
	private LocalDate firstDate;
}
