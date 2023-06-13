package com.dateplan.dateplan.domain.schedule.repository;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

}
