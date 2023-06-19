package com.dateplan.dateplan.domain.anniversary.repository;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnniversaryRepository extends JpaRepository<Anniversary, Long> {

}
