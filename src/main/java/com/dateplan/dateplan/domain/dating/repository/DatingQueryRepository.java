package com.dateplan.dateplan.domain.dating.repository;

import static com.dateplan.dateplan.domain.couple.entity.QCouple.couple;
import static com.dateplan.dateplan.domain.dating.entity.QDating.dating;

import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class DatingQueryRepository {

	private final JPAQueryFactory queryFactory;

	public List<Dating> findByYearAndMonthOrderByDate(Long coupleId, Integer year, Integer month) {
		return queryFactory
			.selectFrom(dating)
			.join(dating.couple, couple)
			.where(
				coupleIdEq(coupleId)
					.and(startDateTimeLoe(year, month))
					.and(endDateTimeGoe(year, month)))
			.orderBy(dating.startDateTime.asc())
			.fetch();
	}

	private BooleanExpression startDateTimeLoe(Integer year, Integer month) {
		if (year == null && month == null) {
			return null;
		}
		if (year == null) {
			return dating.startDateTime.month().loe(month);
		}
		if (month == null) {
			return dating.startDateTime.year().loe(year);
		}
		return dating.startDateTime.loe(
			YearMonth.of(year, month).atEndOfMonth().atTime(LocalTime.MAX));
	}

	private BooleanExpression endDateTimeGoe(Integer year, Integer month) {
		if (year == null && month == null) {
			return null;
		}
		if (year == null) {
			return dating.endDateTime.month().goe(month);
		}
		if (month == null) {
			return dating.endDateTime.year().goe(year);
		}
		return dating.endDateTime.goe(
			YearMonth.of(year,month).atDay(1).atTime(LocalTime.MIN));
	}

	private BooleanExpression coupleIdEq(Long coupleId) {
		return dating.couple.id.eq(coupleId);
	}

}
