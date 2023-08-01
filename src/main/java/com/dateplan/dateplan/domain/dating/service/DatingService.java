package com.dateplan.dateplan.domain.dating.service;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.domain.dating.repository.DatingRepository;
import com.dateplan.dateplan.domain.dating.service.dto.request.DatingCreateServiceRequest;
import com.dateplan.dateplan.domain.dating.service.dto.request.DatingUpdateServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
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

	public void createDating(Member member, DatingCreateServiceRequest request) {
		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		Dating dating = request.toDatingEntity(couple);
		datingRepository.save(dating);
	}

	public void updateDating(
		Long datingId,
		DatingUpdateServiceRequest request
	) {
		Dating dating = datingReadService.findByDatingId(datingId);

		dating.updateDating(
			request.getTitle(),
			request.getLocation(),
			request.getContent(),
			request.getStartDateTime(),
			request.getEndDateTime()
		);
	}

	public void deleteDating(Long datingId) {
		Dating dating = datingReadService.findByDatingId(datingId);

		datingRepository.delete(dating);
	}
}
