package com.dateplan.dateplan.domain.dating.service;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.domain.dating.repository.DatingRepository;
import com.dateplan.dateplan.domain.dating.service.dto.request.DatingCreateServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatingService {

	private final DatingRepository datingRepository;
	private final CoupleReadService coupleReadService;

	public void createDating(Member member, Long coupleId, DatingCreateServiceRequest request) {
		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		if (isNotSameCouple(coupleId, couple)) {
			throw new NoPermissionException(Resource.COUPLE, Operation.CREATE);
		}

		Dating dating = request.toDatingEntity(couple);
		datingRepository.save(dating);
	}

	private boolean isNotSameCouple(Long coupleId, Couple couple) {
		return !Objects.equals(couple.getId(), coupleId);
	}
}
