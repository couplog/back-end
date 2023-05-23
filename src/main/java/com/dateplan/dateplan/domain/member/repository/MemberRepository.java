package com.dateplan.dateplan.domain.member.repository;

import com.dateplan.dateplan.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByPhone(String phone);

	Optional<Member> findByPhone(String phone);

}
