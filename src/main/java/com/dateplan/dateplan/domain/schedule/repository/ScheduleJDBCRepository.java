package com.dateplan.dateplan.domain.schedule.repository;

import static java.time.temporal.ChronoUnit.MINUTES;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import java.sql.PreparedStatement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class ScheduleJDBCRepository {

	private final JdbcTemplate jdbcTemplate;

	public void processBatchUpdate(
		List<Schedule> schedules,
		String title,
		String location,
		String content,
		long startTimeDiff,
		long endTimeDiff
	) {
		String sql = "UPDATE schedule "
			+ "SET title = ?, "
			+ "location = ?, "
			+ "content = ?, "
			+ "start_date_time = ?, "
			+ "end_date_time = ? "
			+ "WHERE schedule_id = ?";
		jdbcTemplate.batchUpdate(
			sql,
			schedules,
			schedules.size(),
			(PreparedStatement ps, Schedule schedule) -> {
				ps.setString(1, title);
				ps.setString(2, location);
				ps.setString(3, content);
				ps.setObject(4, schedule.getStartDateTime().plus(startTimeDiff, MINUTES));
				ps.setObject(5, schedule.getEndDateTime().plus(endTimeDiff, MINUTES));
				ps.setObject(6, schedule.getId());
			}
		);
	}

	public void processBatchInsert(List<Schedule> schedules) {
		String sql = "INSERT INTO schedule "
			+ "(start_date_time, "
			+ "end_date_time, "
			+ "title, "
			+ "content, "
			+ "location, "
			+ "schedule_pattern_id) "
			+ "VALUES (?, ?, ?, ?, ?, ?)";

		Long schedulePatternId = schedules.get(0).getSchedulePattern().getId();
		jdbcTemplate.batchUpdate(
			sql,
			schedules,
			schedules.size(),
			(PreparedStatement ps, Schedule schedule) -> {
				ps.setObject(1, schedule.getStartDateTime());
				ps.setObject(2, schedule.getEndDateTime());
				ps.setString(3, schedule.getTitle());
				ps.setString(4, schedule.getContent());
				ps.setString(5, schedule.getLocation());
				ps.setLong(6, schedulePatternId);
			}
		);
	}
}
