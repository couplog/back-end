package com.dateplan.dateplan.domain.anniversary.repository;

import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnniversaryPatternRepository extends JpaRepository<AnniversaryPattern, Long> {

	@Modifying
	@Query("delete from AnniversaryPattern a where a.couple.id = :coupleId")
	void deleteAllByCoupleId(@Param("coupleId") Long coupleId);
}
