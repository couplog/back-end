package com.dateplan.dateplan.domain.member.service.dto.response;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CoupleConnectServiceResponse {

	private Long coupleId;
	private Long member1Id;
	private Long member2Id;

	public static CoupleConnectServiceResponse from(Couple couple){

		return CoupleConnectServiceResponse.builder()
			.coupleId(couple.getId())
			.member1Id(couple.getMember1().getId())
			.member2Id(couple.getMember2().getId())
			.build();
	}
}
