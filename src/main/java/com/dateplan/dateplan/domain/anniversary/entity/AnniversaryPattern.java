package com.dateplan.dateplan.domain.anniversary.entity;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "anniversary_pattern")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnniversaryPattern {

	@Id
	@Column(name = "anniversary_pattern_id", columnDefinition = "BIGINT", updatable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@Column(name = "repeat_start_date", columnDefinition = "DATE", updatable = false)
	private LocalDate repeatStartDate;

	@NotNull
	@Column(name = "repeat_end_date", columnDefinition = "DATE default '2049-12-31'", updatable = false)
	private LocalDate repeatEndDate;

	@NotNull
	@Enumerated(value = EnumType.STRING)
	@Column(name = "repeat_rule", columnDefinition = "VARCHAR(20)", updatable = false)
	private AnniversaryRepeatRule repeatRule;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, optional = false)
	@JoinColumn(name = "couple_id")
	private Couple couple;

	@Builder
	public AnniversaryPattern(LocalDate repeatStartDate, LocalDate repeatEndDate,
		AnniversaryRepeatRule repeatRule, Couple couple) {
		this.repeatStartDate = repeatStartDate;
		this.repeatEndDate = repeatEndDate;
		this.repeatRule = repeatRule;
		this.couple = couple;
	}
}
