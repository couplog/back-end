package com.dateplan.dateplan.domain.schedule.repository;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class ScheduleJDBCRepository {

	private final ScheduleRepository scheduleRepository;
	private final JdbcTemplate jdbcTemplate;

	public void processBatchInsert(SchedulePattern schedulePattern, List<Schedule> schedules) {
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
}
