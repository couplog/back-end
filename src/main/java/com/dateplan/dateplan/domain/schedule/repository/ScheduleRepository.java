package com.dateplan.dateplan.domain.schedule.repository;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	List<Schedule> findBySchedulePatternId(Long schedulePatternId);
}
