package com.dateplan.dateplan.domain.schedule.repository;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	Boolean existsBySchedulePatternId(Long schedulePatternId);

	@Modifying
	@Query("delete from Schedule s where s.schedulePattern.id = :schedulePatternId")
	void deleteAllBySchedulePatternId(@Param("schedulePatternId") Long id);
}
