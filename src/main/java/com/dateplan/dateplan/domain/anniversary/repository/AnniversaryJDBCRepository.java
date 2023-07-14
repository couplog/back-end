package com.dateplan.dateplan.domain.anniversary.repository;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AnniversaryJDBCRepository {

	private final JdbcTemplate jdbcTemplate;

	@Transactional
	public void saveAll(List<Anniversary> anniversaries){

		String sql = "INSERT INTO anniversary (title, content, date, anniversary_pattern_id) "
			+ "VALUES (?, ?, ?, ?)";

		jdbcTemplate.batchUpdate(sql,
			anniversaries,
			anniversaries.size(),
			(PreparedStatement ps, Anniversary anniversary) -> {
				ps.setString(1, anniversary.getTitle());
				ps.setString(2, anniversary.getContent());
				ps.setDate(3, Date.valueOf(anniversary.getDate()));
				ps.setLong(4, anniversary.getAnniversaryPattern().getId());
			});
	}
}
