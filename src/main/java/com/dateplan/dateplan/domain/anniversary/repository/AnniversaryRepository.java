package com.dateplan.dateplan.domain.anniversary.repository;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnniversaryRepository extends JpaRepository<Anniversary, Long> {

	@Modifying
	@Query("DELETE FROM Anniversary a WHERE a.anniversaryPattern.id = :anniversaryPatternId")
	void deleteAllByAnniversaryPatternId(@Param("anniversaryPatternId") Long anniversaryPatternId);
}
