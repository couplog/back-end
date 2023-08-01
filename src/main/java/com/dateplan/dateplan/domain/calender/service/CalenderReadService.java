package com.dateplan.dateplan.domain.calender.service;

import com.dateplan.dateplan.domain.anniversary.service.AnniversaryReadService;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryDatesServiceResponse;
import com.dateplan.dateplan.domain.calender.service.dto.response.CalenderDateServiceResponse;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.dating.service.DatingReadService;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingDatesServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.service.ScheduleReadService;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleDatesServiceResponse;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CalenderReadService {

	private final CoupleReadService coupleReadService;
	private final DatingReadService datingReadService;
	private final ScheduleReadService scheduleReadService;
	private final AnniversaryReadService anniversaryReadService;

	public CalenderDateServiceResponse readCalenderDates(Member member, Long memberId,
		Integer year, Integer month) {

		if (!Objects.equals(member.getId(), memberId)) {
			throw new NoPermissionException(Resource.MEMBER, Operation.READ);
		}
		Long coupleId = coupleReadService.getCoupleInfo(member).getCoupleId();
		Long partnerId = coupleReadService.getPartnerId(member);

		DatingDatesServiceResponse datingDates = datingReadService.readDatingDates(
			member, coupleId, year, month);
		ScheduleDatesServiceResponse myScheduleDates = scheduleReadService.readScheduleDates(
			memberId, year, month);
		ScheduleDatesServiceResponse partnerScheduleDates = scheduleReadService.readScheduleDates(
			partnerId, year, month);
		AnniversaryDatesServiceResponse anniversaryDates = anniversaryReadService.readAnniversaryDates(
			coupleId, year, month);
		return CalenderDateServiceResponse.of(datingDates, myScheduleDates, partnerScheduleDates,
			anniversaryDates);
	}
}
