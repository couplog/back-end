package com.dateplan.dateplan.domain.anniversary.service;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryQueryRepository;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryDatesServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryListServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.ComingAnniversaryListServiceResponse;
import com.dateplan.dateplan.global.exception.anniversary.AnniversaryNotFoundException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AnniversaryReadService {

	private final AnniversaryQueryRepository anniversaryQueryRepository;

	public AnniversaryListServiceResponse readAnniversaries(Long targetCoupleId, Integer year,
		Integer month, Integer day, boolean onlyRepeatStarted) {

		List<Anniversary> anniversaries = anniversaryQueryRepository.findAllByCoupleIdAndDateInfo(
			targetCoupleId, year, month, day, true, onlyRepeatStarted);

		return AnniversaryListServiceResponse.from(anniversaries);
	}

	public ComingAnniversaryListServiceResponse readComingAnniversaries(Long targetCoupleId,
		LocalDate startDate, Integer size) {

		List<Anniversary> anniversaries = anniversaryQueryRepository.findAllComingAnniversariesByCoupleId(
			startDate, targetCoupleId, size);

		return ComingAnniversaryListServiceResponse.from(anniversaries);
	}

	public AnniversaryDatesServiceResponse readAnniversaryDates(Long targetCoupleId, Integer year,
		Integer month) {

		List<Anniversary> anniversaries = anniversaryQueryRepository.findAllByCoupleIdAndDateInfo(
			targetCoupleId, year, month, null, false, false);

		return AnniversaryDatesServiceResponse.from(anniversaries);
	}

	public Anniversary findAnniversaryByIdOrElseThrow(Long anniversaryId,
		boolean patternFetchJoinRequired) {

		return anniversaryQueryRepository.findById(anniversaryId, patternFetchJoinRequired)
			.orElseThrow(AnniversaryNotFoundException::new);
	}
}
