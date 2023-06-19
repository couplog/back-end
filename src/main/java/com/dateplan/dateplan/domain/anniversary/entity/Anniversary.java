package com.dateplan.dateplan.domain.anniversary.entity;

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
@Table(name = "anniversary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Anniversary {

	@Id
	@Column(name = "anniversary_id", columnDefinition = "BIGINT", updatable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@Column(name = "title", columnDefinition = "VARCHAR(15)")
	private String title;

	@Column(name = "content", columnDefinition = "VARCHAR(100)")
	private String content;

	@NotNull
	@Column(name = "date", columnDefinition = "DATE")
	private LocalDate date;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "category", columnDefinition = "VARCHAR(20)", updatable = false)
	private AnniversaryCategory category;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, optional = false)
	@JoinColumn(name = "anniversary_pattern_id")
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

	public static Anniversary ofOther(String title, String content, LocalDate date, AnniversaryPattern anniversaryPattern){

		return Anniversary.builder()
			.title(title)
			.content(content)
			.anniversaryPattern(anniversaryPattern)
			.date(date)
			.category(AnniversaryCategory.OTHER)
			.build();
	}

	public static Anniversary ofFirstDate(String title, LocalDate date,
		AnniversaryPattern anniversaryPattern) {

		return Anniversary.builder()
			.title(title)
			.date(date)
			.anniversaryPattern(anniversaryPattern)
			.category(AnniversaryCategory.FIRST_DATE)
			.build();
	}

	public static Anniversary ofBirthDay(String title, LocalDate date,
		AnniversaryPattern anniversaryPattern) {

		return Anniversary.builder()
			.title(title)
			.date(date)
			.anniversaryPattern(anniversaryPattern)
			.category(AnniversaryCategory.BIRTH)
			.build();
	}
}
