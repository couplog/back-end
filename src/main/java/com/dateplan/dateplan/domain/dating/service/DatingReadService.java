package com.dateplan.dateplan.domain.dating.service;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.domain.dating.repository.DatingQueryRepository;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingDatesServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class DatingReadService {

	private final DatingQueryRepository datingQueryRepository;
	private final CoupleReadService coupleReadService;

	public DatingDatesServiceResponse readDatingDates(
		Member member,
		Long coupleId,
		Integer year,
		Integer month
	) {
		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		if (isNotSameCouple(coupleId, couple.getId())) {
			throw new NoPermissionException(Resource.COUPLE, Operation.READ);
		}

		List<Dating> datingList = datingQueryRepository.findByYearAndMonthOrderByDate(coupleId,
			year, month);
		return DatingDatesServiceResponse.builder()
			.datingDates(getDatingDates(year, month, datingList))
			.build();
	}

	private List<LocalDate> getDatingDates(
		Integer year,
		Integer month,
		List<Dating> datingList
	) {
		return datingList.stream()
			.flatMap(this::getDatingDateRange)
			.filter(date -> checkDateRange(year, month, date))
			.distinct()
			.sorted(LocalDate::compareTo)
			.toList();
	}

	private Stream<LocalDate> getDatingDateRange(Dating dating) {
		LocalDate startDate = dating.getStartDateTime().toLocalDate();
		LocalDate endDate = dating.getEndDateTime().toLocalDate();
		return startDate.datesUntil(endDate.plusDays(1));
	}

	private boolean checkDateRange(Integer year, Integer month, LocalDate date) {
		if (year == null && month == null) {
			return true;
		}
		if (year != null && month == null) {
			return date.getYear() == year;
		}
		if (year == null) {
			return date.getMonthValue() == month;
		}
		return date.getYear() == year && date.getMonthValue() == month;
	}

	private boolean isNotSameCouple(Long requestId, Long coupleId) {
		return !Objects.equals(requestId, coupleId);
	}
}
