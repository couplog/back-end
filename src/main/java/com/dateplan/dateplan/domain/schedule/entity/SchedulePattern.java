package com.dateplan.dateplan.domain.schedule.entity;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.constant.RepeatRule;
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
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "schedule_pattern")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SchedulePattern {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "schedule_pattern_id", columnDefinition = "BIGINT", updatable = false, unique = true)
	private Long id;

	@NotNull
	@Column(name = "repeat_start_date", columnDefinition = "DATE")
	private LocalDate repeatStartDate;

	@NotNull
	@Column(name = "repeat_end_date", columnDefinition = "DATE")
	@ColumnDefault("'2049-12-31'")
	private LocalDate repeatEndDate;

	@NotNull
	@Column(name = "repeat_rule", columnDefinition = "CHAR(1)")
	@Enumerated(EnumType.STRING)
	private RepeatRule repeatRule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Builder
	public SchedulePattern(
		LocalDate repeatStartDate,
		LocalDate repeatEndDate,
		RepeatRule repeatRule,
		Member member
	) {
		this.repeatStartDate = repeatStartDate;
		this.repeatEndDate = repeatEndDate;
		this.repeatRule = repeatRule;
		this.member = member;
	}

}
