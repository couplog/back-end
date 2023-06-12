package com.dateplan.dateplan.domain.couple.service;

import com.dateplan.dateplan.domain.couple.dto.CoupleInfoServiceResponse;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CoupleReadService {

	private final CoupleRepository coupleRepository;

	public boolean isMemberConnected(Member member) {
		return coupleRepository.existsByMember1OrMember2(member, member);
	}

	public Couple findCoupleByMemberOrElseThrow(Member member) {
		return coupleRepository.findByMember1OrMember2(member, member)
			.orElseThrow(MemberNotConnectedException::new);
	}

	public boolean isCurrentLoginMemberConnected() {

		Member loginMember = MemberThreadLocal.get();

		return coupleRepository.existsByMember1OrMember2(loginMember, loginMember);
	}

	public Long getPartnerId(Member member){

		return findCoupleByMemberOrElseThrow(member)
			.getPartnerId(member);
	}

	public CoupleInfoServiceResponse getCoupleInfo() {
		final Member member = MemberThreadLocal.get();

		Couple couple = findCoupleByMemberOrElseThrow(member);
		Long partnerId = couple.getPartnerId(member);

		return CoupleInfoServiceResponse.builder()
			.coupleId(couple.getId())
			.partnerId(partnerId)
			.firstDate(couple.getFirstDate())
			.build();
	}
}
