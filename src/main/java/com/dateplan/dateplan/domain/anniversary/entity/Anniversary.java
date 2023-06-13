package com.dateplan.dateplan.domain.anniversary.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "anniversary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Anniversary {

	@Id
	@Column(name = "anniversary_id", columnDefinition = "BIGINT", updatable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", columnDefinition = "VARCHAR(15)", nullable = false)
	private String title;

	@Column(name = "content", columnDefinition = "VARCHAR(100)")
	private String content;

	@Column(name = "date", columnDefinition = "DATETIME", nullable = false)
	private LocalDate date;

	@Column(name = "category", columnDefinition = "VARCHAR(20)", nullable = false, updatable = false)
	private AnniversaryCategory category;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, optional = false)
	@JoinColumn(name = "anniversary_pattern_id", nullable = false)
	private AnniversaryPattern anniversaryPattern;

	@Builder
	public Anniversary(String title, String content, LocalDate date, AnniversaryCategory category,
		AnniversaryPattern anniversaryPattern) {
		this.title = title;
		this.content = content;
		this.date = date;
		this.category = category;
		this.anniversaryPattern = anniversaryPattern;
	}
}
