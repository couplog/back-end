package com.dateplan.dateplan.domain.schedule.service;

import static com.dateplan.dateplan.global.util.ScheduleDateUtil.getNextCycle;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceRequest;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleUpdateServiceRequest;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleJDBCRepository;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {

	private final SchedulePatternRepository schedulePatternRepository;
	private final ScheduleJDBCRepository scheduleJDBCRepository;
	private final ScheduleReadService scheduleReadService;

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

	public void updateSchedule(
		Long memberId,
		Long scheduleId,
		ScheduleUpdateServiceRequest request,
		Member member,
		Boolean updateRepeat
	) {
		if (!isSameMember(memberId, member.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.UPDATE);
		}
		Schedule schedule = scheduleReadService.findScheduleByIdOrElseThrow(scheduleId);
		if (updateRepeat) {
			updateRepeatSchedules(request, schedule);
			return;
		}
		updateSingleSchedule(request, schedule);
	}

	private void updateRepeatSchedules(
		ScheduleUpdateServiceRequest request,
		Schedule schedule
	) {
		long startTimeDiff = ChronoUnit.MINUTES.between(schedule.getStartDateTime(),
			request.getStartDateTime());
		long endTimeDiff = ChronoUnit.MINUTES.between(schedule.getEndDateTime(),
			request.getEndDateTime());
		List<Schedule> schedules = scheduleReadService.findBySchedulePatternId(
			schedule.getSchedulePattern().getId());
		scheduleJDBCRepository.processBatchUpdate(schedules, request.getTitle(), request.getLocation(), request.getContent(), startTimeDiff, endTimeDiff);
	}

	private void updateSingleSchedule(ScheduleUpdateServiceRequest request, Schedule schedule) {
		schedule.updateSchedule(
			request.getTitle(),
			request.getContent(),
			request.getLocation(),
			request.getStartDateTime(),
			request.getEndDateTime()
		);
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
