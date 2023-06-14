package com.dateplan.dateplan.domain.schedule.service;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceRequest;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleRepository;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.util.ScheduleDateUtil;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {

	private final JdbcTemplate jdbcTemplate;
	private final SchedulePatternRepository schedulePatternRepository;
	private final ScheduleRepository scheduleRepository;

	public void createSchedule(Long memberId, ScheduleServiceRequest request) {
		request.setDefaultRepeatEndTime();
		request.checkValidation();
		Member member = MemberThreadLocal.get();
		if (!isSameMember(memberId, member.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.CREATE);
		}

		SchedulePattern schedulePattern = buildSchedulePatternEntity(request, member);
		schedulePatternRepository.save(schedulePattern);

		List<Schedule> schedules = getSchedules(request, schedulePattern);

		processBatchInsert(schedulePattern, schedules);
	}

	private List<Schedule> getSchedules(ScheduleServiceRequest request,
		SchedulePattern schedulePattern) {
		List<Schedule> schedules = new ArrayList<>();
		LocalDateTime now = request.getStartDateTime();
		while (isBeforeOfRepeatEndDate(request.getRepeatEndTime(), now.toLocalDate())) {
			schedules.add(buildScheduleEntity(now, request, schedulePattern));
			if (request.getRepeatRule().equals(RepeatRule.N)) {
				break;
			}
			now = ScheduleDateUtil.getNextCycle(now, request.getRepeatRule());
		}

		return schedules;
	}

	private void processBatchInsert(SchedulePattern schedulePattern, List<Schedule> schedules) {
		String sql = "INSERT INTO schedule "
			+ "(schedule_id, "
			+ "start_date_time, "
			+ "end_date_time, "
			+ "title, "
			+ "content, "
			+ "location, "
			+ "schedule_pattern_id) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

		Long maxId = scheduleRepository.findTopByOrderByIdDesc()
			.orElse(0L);

		jdbcTemplate.batchUpdate(sql, getBatchSetter(schedulePattern, schedules, maxId));
	}

	private boolean isBeforeOfRepeatEndDate(LocalDate repeatEndTime, LocalDate now) {
		return !now.isAfter(repeatEndTime);
	}

	private BatchPreparedStatementSetter getBatchSetter(SchedulePattern schedulePattern,
		List<Schedule> schedules, Long maxId) {
		return new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, maxId + i + 1);
				ps.setObject(2, schedules.get(i).getStartDateTime());
				ps.setObject(3, schedules.get(i).getEndDateTime());
				ps.setString(4, schedules.get(i).getTitle());
				ps.setString(5, schedules.get(i).getContent());
				ps.setString(6, schedules.get(i).getLocation());
				ps.setLong(7, schedulePattern.getId());
			}

			@Override
			public int getBatchSize() {
				return schedules.size();
			}
		};
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
