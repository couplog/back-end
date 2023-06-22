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

	public List<Schedule> findByYearAndMonthOrderByDate(Long memberId, Integer year, Integer month) {
		return queryFactory
			.select(schedule)
			.from(schedule)
			.join(schedule.schedulePattern, schedulePattern)
			.join(schedulePattern.member, member)
			.where(memberIdEq(memberId)
				.and(yearEq(year))
				.and(monthEq(month)))
			.orderBy(schedule.startDateTime.asc())
			.fetch();
	}

	private BooleanExpression memberIdEq(Long memberId) {
		return schedulePattern.member.id.eq(memberId);
	}

	private BooleanExpression yearEq(Integer year) {
		if (year == null) {
			return null;
		}
		return schedule.startDateTime.year().eq(year);
	}

	private BooleanExpression monthEq(Integer month) {
		if (month == null) {
			return null;
		}
		return schedule.startDateTime.month().eq(month);
	}
}
