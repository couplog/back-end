package com.dateplan.dateplan.domain.schedule.repository;

import static com.dateplan.dateplan.domain.member.entity.QMember.member;
import static com.dateplan.dateplan.domain.schedule.entity.QSchedule.schedule;
import static com.dateplan.dateplan.domain.schedule.entity.QSchedulePattern.schedulePattern;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class ScheduleQueryRepository {

	private final JPAQueryFactory queryFactory;

	public void deleteByMemberId(Long memberId) {
		queryFactory.delete(schedule)
			.where(schedule.schedulePattern.in(
				JPAExpressions.selectFrom(schedulePattern)
					.join(schedulePattern.member, member)
					.where(memberIdEq(memberId))
					.select(schedulePattern)))
			.execute();
	}

	public Optional<LocalDateTime> findMinStartDateTimeBySchedulePatternId(Long schedulePatternId) {
		return Optional.ofNullable(queryFactory
			.select(schedule.startDateTime.min())
			.from(schedule)
			.join(schedule.schedulePattern, schedulePattern)
			.where(schedule.schedulePattern.id.eq(schedulePatternId))
			.fetchOne());
	}

	public Optional<LocalDateTime> findMaxStartDateTimeBySchedulePatternId(Long schedulePatternId) {
		return Optional.ofNullable(queryFactory
			.select(schedule.startDateTime.max())
			.from(schedule)
			.join(schedule.schedulePattern, schedulePattern)
			.where(schedule.schedulePattern.id.eq(schedulePatternId))
			.fetchOne());
	}

	public List<Schedule> findByDateBetween(Long memberId, Integer year, Integer month,
		Integer day) {
		return queryFactory
			.selectFrom(schedule)
			.join(schedule.schedulePattern, schedulePattern)
			.fetchJoin()
			.join(schedulePattern.member, member)
			.where(memberIdEq(memberId)
				.and(dateBetween(year, month, day)))
			.orderBy(schedule.startDateTime.asc())
			.fetch();
	}

	public List<Schedule> findByYearAndMonthOrderByDate(Long memberId, Integer year,
		Integer month) {
		return queryFactory
			.select(schedule)
			.from(schedule)
			.join(schedule.schedulePattern, schedulePattern)
			.join(schedulePattern.member, member)
			.where(memberIdEq(memberId)
				.and(startDateTimeLoe(year, month))
				.and(endDateTimeGoe(year, month)))
			.orderBy(schedule.startDateTime.asc())
			.fetch();
	}

	public Optional<Schedule> findById(Long scheduleId) {
		return Optional.ofNullable(
			queryFactory
				.selectFrom(schedule)
				.join(schedule.schedulePattern, schedulePattern)
				.fetchJoin()
				.where(schedule.id.eq(scheduleId))
				.fetchOne()
		);
	}

	private BooleanExpression dateBetween(Integer year, Integer month, Integer day) {
		LocalDate requestDate = LocalDate.of(year, month, day);
		return schedule.startDateTime.loe(requestDate.atTime(LocalTime.MAX))
			.and(schedule.endDateTime.goe(requestDate.atTime(LocalTime.MIN)));
	}

	private BooleanExpression memberIdEq(Long memberId) {
		return schedulePattern.member.id.eq(memberId);
	}

	private BooleanExpression startDateTimeLoe(Integer year, Integer month) {
		if (year == null && month == null) {
			return null;
		}
		if (year == null) {
			return schedule.startDateTime.month().loe(month);
		}
		if (month == null) {
			return schedule.startDateTime.year().loe(year);
		}
		return schedule.startDateTime.loe(
			YearMonth.of(year, month).atEndOfMonth().atTime(LocalTime.MAX));
	}

	private BooleanExpression endDateTimeGoe(Integer year, Integer month) {
		if (year == null && month == null) {
			return null;
		}
		if (year == null) {
			return schedule.endDateTime.month().goe(month);
		}
		if (month == null) {
			return schedule.endDateTime.year().goe(year);
		}
		return schedule.endDateTime.goe(
			YearMonth.of(year, month).atDay(1).atTime(LocalTime.MIN));
	}
}
