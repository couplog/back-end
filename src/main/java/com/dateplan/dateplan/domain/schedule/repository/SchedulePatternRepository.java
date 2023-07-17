package com.dateplan.dateplan.domain.schedule.repository;

import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchedulePatternRepository extends JpaRepository<SchedulePattern, Long> {

	@Modifying
	@Query("delete from SchedulePattern s where s.member.id = :memberId")
	void deleteAllByMemberId(@Param("memberId") Long memberId);
}
