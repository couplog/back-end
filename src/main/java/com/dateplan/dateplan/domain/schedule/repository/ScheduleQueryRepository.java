package com.dateplan.dateplan.domain.schedule.repository;

import static com.dateplan.dateplan.domain.member.entity.QMember.member;
import static com.dateplan.dateplan.domain.schedule.entity.QSchedule.schedule;
import static com.dateplan.dateplan.domain.schedule.entity.QSchedulePattern.schedulePattern;

import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class ScheduleQueryRepository {

	private final JPAQueryFactory queryFactory;

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
		return schedule.startDateTime.year().loe(year)
			.and(schedule.startDateTime.month().loe(month));
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
		return schedule.endDateTime.year().goe(year)
			.and(schedule.endDateTime.month().goe(month));
	}
}
