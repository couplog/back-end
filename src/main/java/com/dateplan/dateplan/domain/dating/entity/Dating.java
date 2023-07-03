package com.dateplan.dateplan.domain.dating.entity;

import com.dateplan.dateplan.domain.couple.entity.Couple;
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
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "dating")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
public class Dating {

	@Id
	@Column(name = "dating_id", columnDefinition = "BIGINT", updatable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@Column(name = "title", columnDefinition = "VARCHAR(15)")
	private String title;

	@Column(name = "location", columnDefinition = "VARCHAR(20)")
	private String location;

	@Column(name = "content", columnDefinition = "VARCHAR(80)")
	private String content;

	@NotNull
	@Column(name = "start_date_time", columnDefinition = "DATETIME")
	private LocalDateTime startDateTime;

	@NotNull
	@Column(name = "end_date_time", columnDefinition = "DATETIME")
	private LocalDateTime endDateTime;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "couple_id", nullable = false)
	private Couple couple;

	@Builder
	public Dating(
		String title,
		String location,
		String content,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime,
		Couple couple
	) {
		this.title = title;
		this.location = location;
		this.content = content;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.couple = couple;
	}
}
