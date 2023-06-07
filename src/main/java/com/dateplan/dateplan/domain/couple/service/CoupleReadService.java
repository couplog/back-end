package com.dateplan.dateplan.domain.couple.service;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.exception.couple.CoupleNotConnectedException;
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
			.orElseThrow(CoupleNotConnectedException::new);
	}

}
