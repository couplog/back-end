package com.dateplan.dateplan.domain.schedule.service;

import static com.dateplan.dateplan.global.util.ScheduleDateUtil.getNextCycle;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceRequest;
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
			if (nextCycle.getDayOfMonth() == now.getDayOfMonth()) {
				schedules.add(request.toScheduleEntity(nextCycle, schedulePattern));
			}
		}

		return schedules;
	}

	private boolean isBeforeOfRepeatEndDate(LocalDate repeatEndTime, LocalDateTime now) {
		return !now.toLocalDate().isAfter(repeatEndTime);
	}

	private boolean isSameMember(Long memberId, Long loginMemberId) {

		return Objects.equals(memberId, loginMemberId);
	}
}