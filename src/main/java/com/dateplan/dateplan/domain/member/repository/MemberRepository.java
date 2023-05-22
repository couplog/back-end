package com.dateplan.dateplan.domain.member.repository;

import com.dateplan.dateplan.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByPhone(String phone);
}
