package com.dateplan.dateplan.domain.schedule.repository;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
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

	private final JdbcTemplate jdbcTemplate;

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

		jdbcTemplate.batchUpdate(sql, getBatchSetter(schedulePatternId, schedules));
	}

	private BatchPreparedStatementSetter getBatchSetter(Long schedulePatternId,
		List<Schedule> schedules) {
		return new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setObject(1, schedules.get(i).getStartDateTime());
				ps.setObject(2, schedules.get(i).getEndDateTime());
				ps.setString(3, schedules.get(i).getTitle());
				ps.setString(4, schedules.get(i).getContent());
				ps.setString(5, schedules.get(i).getLocation());
				ps.setLong(6, schedulePatternId);
			}

			@Override
			public int getBatchSize() {
				return schedules.size();
			}
		};
	}
}
