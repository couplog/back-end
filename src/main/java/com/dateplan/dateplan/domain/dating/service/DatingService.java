package com.dateplan.dateplan.domain.dating.service;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.domain.dating.repository.DatingRepository;
import com.dateplan.dateplan.domain.dating.service.dto.request.DatingCreateServiceRequest;
import com.dateplan.dateplan.domain.dating.service.dto.request.DatingUpdateServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DatingService {

	private final DatingRepository datingRepository;
	private final CoupleReadService coupleReadService;
	private final DatingReadService datingReadService;

	public void createDating(Member member, Long coupleId, DatingCreateServiceRequest request) {
		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		if (isNotSameCouple(coupleId, couple.getId())) {
			throw new NoPermissionException(Resource.COUPLE, Operation.CREATE);
		}

		Dating dating = request.toDatingEntity(couple);
		datingRepository.save(dating);
	}

	public void updateDating(
		Member member,
		Long coupleId,
		Long datingId,
		DatingUpdateServiceRequest request
	) {
		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		if (isNotSameCouple(coupleId, couple.getId())) {
			throw new NoPermissionException(Resource.COUPLE, Operation.UPDATE);
		}

		Dating dating = datingReadService.findByDatingId(datingId);
		dating.updateDating(
			request.getTitle(),
			request.getLocation(),
			request.getContent(),
			request.getStartDateTime(),
			request.getEndDateTime()
		);
	}

	public void deleteDating(Member member, Long coupleId, Long datingId) {
		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		if (isNotSameCouple(coupleId, couple.getId())) {
			throw new NoPermissionException(Resource.COUPLE, Operation.DELETE);
		}

		Dating dating = datingReadService.findByDatingId(datingId);
		datingRepository.delete(dating);
	}

	private boolean isNotSameCouple(Long requestId, Long coupleId) {
		return !Objects.equals(requestId, coupleId);
	}
}
