package com.dateplan.dateplan.domain.schedule.repository;

import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulePatternRepository extends JpaRepository<SchedulePattern, Long> {

}
