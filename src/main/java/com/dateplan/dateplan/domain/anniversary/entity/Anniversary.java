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
	@Column(name = "title", columnDefinition = "VARCHAR(10)")
	private String title;

	@Column(name = "content", columnDefinition = "VARCHAR(80)")
	private String content;

	@NotNull
	@Column(name = "date", columnDefinition = "DATE")
	private LocalDate date;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, optional = false)
	@JoinColumn(name = "anniversary_pattern_id")
	private AnniversaryPattern anniversaryPattern;

	@Builder
	public Anniversary(String title, String content, LocalDate date,
		AnniversaryPattern anniversaryPattern) {
		this.title = title;
		this.content = content;
		this.date = date;
		this.anniversaryPattern = anniversaryPattern;
	}

	public static Anniversary of(AnniversaryPattern anniversaryPattern, String title,
		String content, LocalDate date) {

		return Anniversary.builder()
			.title(title)
			.content(content)
			.date(date)
			.anniversaryPattern(anniversaryPattern)
			.build();
	}

	public static Anniversary ofBirthDay(AnniversaryPattern anniversaryPattern, LocalDate birthDay,
		String memberName) {

		return Anniversary.builder()
			.title(memberName + " 님의 생일")
			.date(birthDay)
			.anniversaryPattern(anniversaryPattern)
			.build();
	}

	public static Anniversary ofFirstDate(AnniversaryPattern anniversaryPattern,
		LocalDate firstDate, Integer timeDifference) {

		AnniversaryBuilder anniversaryBuilder = Anniversary.builder()
			.anniversaryPattern(anniversaryPattern);

		AnniversaryRepeatRule repeatRule = anniversaryPattern.getRepeatRule();

		switch (repeatRule) {
			case NONE -> anniversaryBuilder
				.title("처음만난날")
				.date(firstDate);
			case YEAR -> anniversaryBuilder
				.title("만난지 " + timeDifference + "주년")
				.date(firstDate.plusYears(timeDifference));
			case HUNDRED_DAYS -> anniversaryBuilder
				.title("만난지 " + timeDifference + "일")
				.date(firstDate.plusDays(timeDifference));
		}

		return anniversaryBuilder.build();
	}
}
