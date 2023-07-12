package com.dateplan.dateplan.domain.schedule.service;

import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleQueryRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleRepository;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleDatesServiceResponse;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleServiceResponse;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.schedule.ScheduleNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ScheduleReadService {

	private final ScheduleQueryRepository scheduleQueryRepository;
	private final CoupleReadService coupleReadService;
	private final ScheduleRepository scheduleRepository;

	public Optional<LocalDateTime> findMinStartDateTimeBySchedulePatternId(Long schedulePatternId) {
		return scheduleQueryRepository.findMinStartDateTimeBySchedulePatternId(schedulePatternId);
	}

	public Optional<LocalDateTime> findMaxStartDateTimeBySchedulePatternId(Long schedulePatternId) {
		return scheduleQueryRepository.findMaxStartDateTimeBySchedulePatternId(schedulePatternId);
	}

	public List<Schedule> findBySchedulePatternId(Long schedulePatternId) {
		return scheduleRepository.findBySchedulePatternId(schedulePatternId);
	}

	public boolean existsBySchedulePatternId(Long schedulePatternId) {
		return scheduleRepository.existsBySchedulePatternId(schedulePatternId);
	}

	public Schedule findScheduleByIdOrElseThrow(Long id) {
		return scheduleQueryRepository.findById(id)
			.orElseThrow(ScheduleNotFoundException::new);
	}

	public ScheduleServiceResponse readSchedules(
		Long requestId,
		Member member,
		Integer year,
		Integer month,
		Integer day
	) {
		Long partnerId = coupleReadService.getPartnerId(member);
		validatePermission(requestId, member.getId(), partnerId);
		List<Schedule> schedules = scheduleQueryRepository.findByDateBetween(requestId,
			year, month, day);
		return ScheduleServiceResponse.from(schedules);
	}

	public ScheduleDatesServiceResponse readScheduleDates(
		Member loginMember,
		Long requestId,
		Integer year,
		Integer month
	) {

		Long partnerId = coupleReadService.getPartnerId(loginMember);
		validatePermission(requestId, loginMember.getId(), partnerId);

		List<Schedule> schedules = scheduleQueryRepository
			.findByYearAndMonthOrderByDate(requestId, year, month);

		return ScheduleDatesServiceResponse.builder()
			.scheduleDates(getScheduleDates(year, month, schedules))
			.build();
	}

	private List<LocalDate> getScheduleDates(
		Integer year,
		Integer month,
		List<Schedule> schedules
	) {
		return schedules.stream()
			.flatMap(this::getScheduleDateRange)
			.filter(date -> checkDateRange(year, month, date))
			.distinct()
			.sorted(LocalDate::compareTo)
			.toList();
	}

	private Stream<LocalDate> getScheduleDateRange(Schedule schedule) {
		LocalDate startDate = schedule.getStartDateTime().toLocalDate();
		LocalDate endDate = schedule.getEndDateTime().toLocalDate();
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

	private void validatePermission(Long requestId, Long memberId, Long partnerId) {
		if (isNotSameMember(requestId, memberId) && isNotSameMember(requestId, partnerId)) {
			throw new NoPermissionException(Resource.MEMBER, Operation.READ);
		}
	}

	private boolean isNotSameMember(Long requestId, Long memberId) {

		return !(Objects.equals(requestId, memberId));
	}
}
