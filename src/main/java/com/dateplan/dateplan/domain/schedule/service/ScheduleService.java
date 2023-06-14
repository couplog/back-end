package com.dateplan.dateplan.domain.schedule.service;

import static com.dateplan.dateplan.global.util.ScheduleDateUtil.*;

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
import com.dateplan.dateplan.global.util.ScheduleDateUtil;
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

	public void createSchedule(Long memberId, ScheduleServiceRequest request) {
		Member member = MemberThreadLocal.get();
		if (!isSameMember(memberId, member.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.CREATE);
		}

		SchedulePattern schedulePattern = buildSchedulePatternEntity(request, member);
		schedulePatternRepository.save(schedulePattern);

		List<Schedule> schedules = getSchedules(request, schedulePattern);

		scheduleJDBCRepository.processBatchInsert(schedulePattern, schedules);
	}

	private List<Schedule> getSchedules(ScheduleServiceRequest request, SchedulePattern schedulePattern) {
		List<Schedule> schedules = new ArrayList<>();

		LocalDateTime now = request.getStartDateTime();
		int count = 1;

		schedules.add(buildScheduleEntity(now, request, schedulePattern));
		if (request.getRepeatRule().equals(RepeatRule.N)) {
			return schedules;
		}

		while (isBeforeOfRepeatEndDate(request.getRepeatEndTime(), getNextCycle(now, request.getRepeatRule(), count))) {
			LocalDateTime nextCycle = getNextCycle(now, request.getRepeatRule(), count++);
			if (nextCycle.getDayOfMonth() == now.getDayOfMonth()) {
				schedules.add(buildScheduleEntity(nextCycle, request, schedulePattern));
			}
		}

		return schedules;
	}

	private boolean isBeforeOfRepeatEndDate(LocalDate repeatEndTime, LocalDateTime now) {
		return !now.toLocalDate().isAfter(repeatEndTime);
	}

	private static SchedulePattern buildSchedulePatternEntity(ScheduleServiceRequest request,
		Member member) {
		return SchedulePattern.builder()
			.repeatStartDate(request.getStartDateTime().toLocalDate())
			.repeatEndDate(request.getRepeatEndTime())
			.member(member)
			.repeatRule(request.getRepeatRule())
			.build();
	}

	private Schedule buildScheduleEntity(LocalDateTime now, ScheduleServiceRequest request,
		SchedulePattern schedulePattern) {
		long diff = ChronoUnit.SECONDS.between(request.getStartDateTime(), request.getEndDateTime());
		return Schedule.builder()
			.startDateTime(now)
			.endDateTime(now.plusSeconds(diff))
			.title(request.getTitle())
			.content(request.getContent())
			.location(request.getLocation())
			.schedulePattern(schedulePattern)
			.build();
	}

	private boolean isSameMember(Long memberId, Long loginMemberId) {

		return Objects.equals(memberId, loginMemberId);
	}
}
