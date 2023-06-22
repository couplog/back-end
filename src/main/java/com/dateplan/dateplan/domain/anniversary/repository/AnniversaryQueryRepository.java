package com.dateplan.dateplan.domain.anniversary.repository;

import static com.dateplan.dateplan.domain.anniversary.entity.QAnniversary.anniversary;
import static com.dateplan.dateplan.domain.anniversary.entity.QAnniversaryPattern.anniversaryPattern;
import static com.dateplan.dateplan.domain.couple.entity.QCouple.couple;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class AnniversaryQueryRepository {

	private final JPAQueryFactory queryFactory;

	public List<Anniversary> findAllByCoupleIdAndYearAndMonth(Long coupleId, Integer year,
		Integer month) {

		if (coupleId == null) {
			return List.of();
		}

		return queryFactory.selectFrom(anniversary)
			.innerJoin(anniversary.anniversaryPattern, anniversaryPattern)
			.innerJoin(anniversaryPattern.couple, couple)
			.where(yearEq(year), monthEq(month), coupleIdEq(coupleId))
			.orderBy(anniversary.date.asc())
			.fetch();
	}

	private BooleanExpression yearEq(Integer year) {

		if (year == null) {
			return null;
		}

		return anniversary.date.year().eq(year);
	}

	private BooleanExpression monthEq(Integer month) {

		if (month == null) {
			return null;
		}

		return anniversary.date.month().eq(month);
	}

	private BooleanExpression coupleIdEq(Long coupleId){

		return couple.id.eq(coupleId);
	}
}
