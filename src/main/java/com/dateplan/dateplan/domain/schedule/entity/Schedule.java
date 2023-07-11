package com.dateplan.dateplan.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schedule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Schedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "schedule_id", columnDefinition = "BIGINT", updatable = false, unique = true)
	private Long id;

	@NotNull
	@Column(name = "start_date_time", columnDefinition = "DATETIME")
	private LocalDateTime startDateTime;

	@NotNull
	@Column(name = "end_date_time", columnDefinition = "DATETIME")
	private LocalDateTime endDateTime;

	@NotNull
	@Column(name = "title", columnDefinition = "VARCHAR(15)")
	private String title;

	@Column(name = "content", columnDefinition = "VARCHAR(80)")
	private String content;

	@Column(name = "location", columnDefinition = "VARCHAR(20)")
	private String location;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "schedule_pattern_id", nullable = false)
	private SchedulePattern schedulePattern;

	@Builder
	public Schedule(
		LocalDateTime startDateTime,
		LocalDateTime endDateTime,
		String title,
		String content,
		String location,
		SchedulePattern schedulePattern
	) {
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.title = title;
		this.content = content;
		this.location = location;
		this.schedulePattern = schedulePattern;
	}

	public void updateSchedule(
		String title,
		String content,
		String location,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime,
		SchedulePattern schedulePattern
	) {
		this.title = title;
		this.content = content;
		this.location = location;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.schedulePattern = schedulePattern;
	}
}
