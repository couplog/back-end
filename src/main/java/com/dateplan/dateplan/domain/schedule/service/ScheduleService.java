package com.dateplan.dateplan.domain.schedule.service;

import static com.dateplan.dateplan.global.util.ScheduleDateUtil.getNextCycle;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceRequest;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceResponse;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleJDBCRepository;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleQueryRepository;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {

	private final SchedulePatternRepository schedulePatternRepository;
	private final ScheduleJDBCRepository scheduleJDBCRepository;
	private final ScheduleQueryRepository scheduleQueryRepository;
	private final CoupleReadService coupleReadService;

	public ScheduleServiceResponse readSchedule(Long memberId, Integer year, Integer month) {
		Member member = MemberThreadLocal.get();

		if (!isSameMember(memberId, member.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.READ);
		}
		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);
		Long partnerId = couple.getPartnerId(member);

		List<Schedule> memberSchedules = scheduleQueryRepository.findByYearAndMonth(memberId, year,
			month);
		List<Schedule> partnerSchedules = scheduleQueryRepository.findByYearAndMonth(partnerId,
			year, month);

		return ScheduleServiceResponse.builder()
			.memberSchedules(getSchedulesDate(memberSchedules, year, month))
			.partnerSchedules(getSchedulesDate(partnerSchedules, year, month))
			.build();
	}

	private List<LocalDate> getSchedulesDate(List<Schedule> schedules, Integer year, Integer month) {
		return schedules.stream()
			.flatMap(schedule -> {
				LocalDate startDate = schedule.getStartDateTime().toLocalDate();
				LocalDate endDate = schedule.getEndDateTime().toLocalDate();
				return Stream.iterate(startDate, date -> date.plusDays(1))
					.limit(ChronoUnit.DAYS.between(startDate, endDate.plusDays(1)))
					.filter(date -> checkDateRange(year, month, date));
			})
			.distinct()
			.collect(Collectors.toList());
	}

	private boolean checkDateRange(Integer year, Integer month, LocalDate startDate) {
		if (year == null && month == null) {
			return true;
		}
		if (year != null && month == null) {
			return startDate.getYear() == year;
		}
		if (year == null) {
			return startDate.getMonthValue() == month;
		}
		return startDate.getYear() == year && startDate.getMonthValue() == month;
	}

	public void createSchedule(Long memberId, ScheduleServiceRequest request) {
		Member member = MemberThreadLocal.get();
		if (!isSameMember(memberId, member.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.CREATE);
		}

		SchedulePattern schedulePattern = request.toSchedulePatternEntity(member);
		schedulePatternRepository.save(schedulePattern);

		List<Schedule> schedules = getSchedules(request, schedulePattern);

		scheduleJDBCRepository.processBatchInsert(schedules);
	}

	private List<Schedule> getSchedules(ScheduleServiceRequest request,
		SchedulePattern schedulePattern) {
		List<Schedule> schedules = new ArrayList<>();

		LocalDateTime now = request.getStartDateTime();
		int count = 1;

		schedules.add(request.toScheduleEntity(now, schedulePattern));
		if (request.getRepeatRule().equals(RepeatRule.N)) {
			return schedules;
		}

		while (isBeforeOfRepeatEndDate(request.getRepeatEndTime(),
			getNextCycle(now, request.getRepeatRule(), count))) {
			LocalDateTime nextCycle = getNextCycle(now, request.getRepeatRule(), count++);
			if (checkNextCycle(request, now, nextCycle)) {
				schedules.add(request.toScheduleEntity(nextCycle, schedulePattern));
			}
		}

		return schedules;
	}

	private boolean checkNextCycle(ScheduleServiceRequest request, LocalDateTime now,
		LocalDateTime nextCycle) {
		if (request.getRepeatRule().equals(RepeatRule.D) ||
			request.getRepeatRule().equals(RepeatRule.W)) {
			return true;
		}
		return nextCycle.getDayOfMonth() == now.getDayOfMonth();
	}

	private boolean isBeforeOfRepeatEndDate(LocalDate repeatEndTime, LocalDateTime now) {
		return !now.toLocalDate().isAfter(repeatEndTime);
	}

	private boolean isSameMember(Long memberId, Long loginMemberId) {

		return Objects.equals(memberId, loginMemberId);
	}
}
