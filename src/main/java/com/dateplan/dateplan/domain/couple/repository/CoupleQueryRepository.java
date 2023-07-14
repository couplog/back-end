package com.dateplan.dateplan.domain.couple.repository;

import static com.dateplan.dateplan.domain.couple.entity.QCouple.couple;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class CoupleQueryRepository {

	private final JPAQueryFactory queryFactory;

	public boolean existsByIdAndMemberId(Long coupleId, Long memberId){

		Couple findCouple = queryFactory.selectFrom(couple)
			.where(couple.member1.id.eq(memberId)
				.or(couple.member2.id.eq(memberId))
				.and(couple.id.eq(coupleId)))
			.limit(1)
			.fetchFirst();

		return findCouple != null;
	}
}
