package com.dateplan.dateplan.domain.schedule.service;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleQueryRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleRepository;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleDatesServiceResponse;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleServiceResponse;
import com.dateplan.dateplan.global.exception.schedule.ScheduleNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ScheduleReadService {

	private final ScheduleQueryRepository scheduleQueryRepository;
	private final ScheduleRepository scheduleRepository;

	public Optional<LocalDateTime> findMinStartDateTimeBySchedulePatternId(Long schedulePatternId) {
		return scheduleQueryRepository.findMinStartDateTimeBySchedulePatternId(schedulePatternId);
	}

	public Optional<LocalDateTime> findMaxStartDateTimeBySchedulePatternId(Long schedulePatternId) {
		return scheduleQueryRepository.findMaxStartDateTimeBySchedulePatternId(schedulePatternId);
	}

	public List<Schedule> findBySchedulePatternId(Long schedulePatternId) {
		return scheduleRepository.findBySchedulePatternId(schedulePatternId);
	}

	public Schedule findScheduleByIdOrElseThrow(Long id) {
		return scheduleQueryRepository.findById(id)
			.orElseThrow(ScheduleNotFoundException::new);
	}

	public ScheduleServiceResponse readSchedules(
		Long requestId,
		Integer year,
		Integer month,
		Integer day
	) {
		List<Schedule> schedules = scheduleQueryRepository.findByDateBetween(requestId,
			year, month, day);
		return ScheduleServiceResponse.from(schedules);
	}

	public ScheduleDatesServiceResponse readScheduleDates(
		Long requestId,
		Integer year,
		Integer month
	) {
		List<Schedule> schedules = scheduleQueryRepository
			.findByYearAndMonthOrderByDate(requestId, year, month);

		return ScheduleDatesServiceResponse.builder()
			.scheduleDates(getScheduleDates(year, month, schedules))
			.build();
	}

	private List<LocalDate> getScheduleDates(
		Integer year,
		Integer month,
		List<Schedule> schedules
	) {
		return schedules.stream()
			.flatMap(this::getScheduleDateRange)
			.filter(date -> checkDateRange(year, month, date))
			.distinct()
			.sorted(LocalDate::compareTo)
			.toList();
	}

	private Stream<LocalDate> getScheduleDateRange(Schedule schedule) {
		LocalDate startDate = schedule.getStartDateTime().toLocalDate();
		LocalDate endDate = schedule.getEndDateTime().toLocalDate();
		return startDate.datesUntil(endDate.plusDays(1));
	}

	private boolean checkDateRange(Integer year, Integer month, LocalDate date) {
		if (year == null && month == null) {
			return true;
		}
		if (year != null && month == null) {
			return date.getYear() == year;
		}
		if (year == null) {
			return date.getMonthValue() == month;
		}
		return date.getYear() == year && date.getMonthValue() == month;
	}
}
