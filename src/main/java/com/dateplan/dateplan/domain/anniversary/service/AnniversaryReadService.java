package com.dateplan.dateplan.domain.anniversary.service;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryQueryRepository;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryDatesServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryListServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.ComingAnniversaryListServiceResponse;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.anniversary.AnniversaryNotFoundException;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AnniversaryReadService {

	private final CoupleReadService coupleReadService;
	private final AnniversaryQueryRepository anniversaryQueryRepository;

	public AnniversaryListServiceResponse readAnniversaries(Member loginMember,
		Long targetCoupleId, Integer year, Integer month, Integer day) {

		Long loginMemberCoupleId = coupleReadService.findCoupleByMemberOrElseThrow(
			loginMember).getId();

		if (!Objects.equals(loginMemberCoupleId, targetCoupleId)) {
			throw new NoPermissionException(Resource.COUPLE, Operation.READ);
		}

		List<Anniversary> anniversaries = anniversaryQueryRepository.findAllByCoupleIdAndDateInfo(
			targetCoupleId, year, month, day, true);

		return AnniversaryListServiceResponse.from(anniversaries);
	}

	public ComingAnniversaryListServiceResponse readComingAnniversaries(Member loginMember,
		Long targetCoupleId, LocalDate startDate, Integer size) {

		Long loginMemberCoupleId = coupleReadService.findCoupleByMemberOrElseThrow(
			loginMember).getId();

		if (!Objects.equals(loginMemberCoupleId, targetCoupleId)) {
			throw new NoPermissionException(Resource.COUPLE, Operation.READ);
		}

		List<Anniversary> anniversaries = anniversaryQueryRepository.findAllComingAnniversariesByCoupleId(
			startDate, targetCoupleId, size);

		return ComingAnniversaryListServiceResponse.from(anniversaries);
	}

	public AnniversaryDatesServiceResponse readAnniversaryDates(Member loginMember,
		Long targetCoupleId, Integer year, Integer month) {

		Long loginMemberCoupleId = coupleReadService.findCoupleByMemberOrElseThrow(
			loginMember).getId();

		if (!Objects.equals(loginMemberCoupleId, targetCoupleId)) {
			throw new NoPermissionException(Resource.COUPLE, Operation.READ);
		}

		List<Anniversary> anniversaries = anniversaryQueryRepository.findAllByCoupleIdAndDateInfo(
			targetCoupleId, year, month, null, false);

		return AnniversaryDatesServiceResponse.from(anniversaries);
	}

	public Anniversary findAnniversaryByIdOrElseThrow(Long anniversaryId, boolean patternFetchJoinRequired) {

		return anniversaryQueryRepository.findById(anniversaryId, patternFetchJoinRequired)
			.orElseThrow(AnniversaryNotFoundException::new);
	}
}
