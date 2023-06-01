package com.dateplan.dateplan.domain.couple.repository;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoupleRepository extends JpaRepository<Couple, Long> {

	Boolean existsByMember1OrMember2(Member member1, Member member2);
}
