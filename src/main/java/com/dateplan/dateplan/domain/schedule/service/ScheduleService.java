package com.dateplan.dateplan.domain.schedule.service;

import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceRequest;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
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

	}

}
