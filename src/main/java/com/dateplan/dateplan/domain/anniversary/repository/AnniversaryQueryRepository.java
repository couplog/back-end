package com.dateplan.dateplan.domain.anniversary.repository;

import static com.dateplan.dateplan.domain.anniversary.entity.QAnniversary.anniversary;
import static com.dateplan.dateplan.domain.anniversary.entity.QAnniversaryPattern.anniversaryPattern;
import static com.dateplan.dateplan.domain.couple.entity.QCouple.couple;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryCategory;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Repository
public class AnniversaryQueryRepository {

	private static final String ADD_DATE_FUNCTION = "ADDDATE({0}, {1})";

	private final JPAQueryFactory queryFactory;

	public void deleteByCoupleId(Long coupleId) {
		queryFactory.delete(anniversary)
			.where(anniversary.anniversaryPattern.in(
				JPAExpressions.selectFrom(anniversaryPattern)
					.join(anniversaryPattern.couple, couple)
					.where(coupleIdEq(coupleId))))
			.execute();
	}

	public List<Anniversary> findAllByCoupleIdAndDateInfo(Long coupleId, Integer year,
		Integer month, Integer day, boolean patternFetchJoinRequired, boolean onlyRepeatStarted) {

		if (coupleId == null) {
			return List.of();
		}

		JPAQuery<Anniversary> baseQuery = queryFactory.selectFrom(anniversary)
			.where(yearEq(year), monthEq(month), dayEq(day), coupleIdEq(coupleId))
			.orderBy(anniversary.date.asc());

		if (patternFetchJoinRequired) {
			baseQuery
				.innerJoin(anniversary.anniversaryPattern, anniversaryPattern)
				.fetchJoin()
				.innerJoin(anniversaryPattern.couple, couple);
		} else {
			baseQuery
				.innerJoin(anniversary.anniversaryPattern, anniversaryPattern)
				.innerJoin(anniversaryPattern.couple, couple);
		}

		if (onlyRepeatStarted) {
			baseQuery
				.where(anniversaryPattern.repeatStartDate.eq(anniversary.date));
		}

		return baseQuery.fetch();
	}

	public List<Anniversary> findAllComingAnniversariesByCoupleId(LocalDate startDate,
		Long coupleId, Integer size) {

		return queryFactory.selectFrom(anniversary)
			.innerJoin(anniversary.anniversaryPattern, anniversaryPattern)
			.innerJoin(anniversaryPattern.couple, couple)
			.where(coupleIdEq(coupleId), startDateGoe(startDate))
			.orderBy(anniversary.date.asc())
			.limit(size)
			.fetch();
	}

	public Optional<Anniversary> findById(Long anniversaryId, boolean patternFetchJoinRequired) {

		JPAQuery<Anniversary> baseQuery = queryFactory.selectFrom(anniversary)
			.where(anniversary.id.eq(anniversaryId));

		if (patternFetchJoinRequired) {
			baseQuery
				.innerJoin(anniversary.anniversaryPattern)
				.fetchJoin();
		}

		return Optional.ofNullable(baseQuery.fetchFirst());
	}

	@Transactional
	public void updateAllRepeatedAnniversary(Long anniversaryId, String title, String content,
		LocalDate date) {

		Anniversary findAnniversary = queryFactory.selectFrom(anniversary)
			.where(anniversary.id.eq(anniversaryId))
			.fetchFirst();

		if (findAnniversary == null) {
			return;
		}

		JPAUpdateClause baseQuery = queryFactory.update(anniversary)
			.where(anniversary.anniversaryPattern.id.eq(
				findAnniversary.getAnniversaryPattern().getId()));

		boolean isTitleChanged = !Objects.equals(findAnniversary.getTitle(), title);
		boolean isContentChanged = !Objects.equals(findAnniversary.getContent(), content);
		boolean isDateChanged = !Objects.equals(findAnniversary.getDate(), date);

		if (!isTitleChanged && !isContentChanged && !isDateChanged) {
			return;
		}

		if (isTitleChanged) {
			baseQuery
				.set(anniversary.title, title);
		}

		if (isContentChanged) {
			baseQuery
				.set(anniversary.content, content);
		}

		if (isDateChanged) {

			long dayDiff = ChronoUnit.DAYS.between(findAnniversary.getDate(), date);

			DateTemplate<LocalDate> dateTemplate = Expressions.dateTemplate(
				LocalDate.class, ADD_DATE_FUNCTION, anniversary.date,
				Expressions.asNumber(dayDiff));

			baseQuery
				.set(anniversary.date, dateTemplate);
		}

		baseQuery.execute();
	}

	@Transactional
	public void updateAllRepeatedAnniversaryForFirstDate(Long coupleId, LocalDate changedDate) {

		List<AnniversaryPattern> anniversaryPatterns = queryFactory.selectFrom(anniversaryPattern)
			.where(anniversaryPattern.couple.id.eq(coupleId),
				anniversaryPattern.category.eq(AnniversaryCategory.FIRST_DATE))
			.fetch();

		List<Long> anniversaryPatternIds = anniversaryPatterns.stream()
			.map(AnniversaryPattern::getId)
			.toList();

		LocalDate beforeChangedFirstDate = anniversaryPatterns.get(0).getRepeatStartDate();

		long dayDiff = ChronoUnit.DAYS.between(beforeChangedFirstDate, changedDate);

		DateTemplate<LocalDate> dateTemplateForAnniversary = createDateTemplateForAnniversary(
			anniversary.date, ADD_DATE_FUNCTION, dayDiff);

		DateTemplate<LocalDate> dateTemplateForAnniversaryPatternRepeatStartDate = createDateTemplateForAnniversary(
			anniversaryPattern.repeatStartDate, ADD_DATE_FUNCTION, dayDiff
		);

		DateTemplate<LocalDate> dateTemplateForAnniversaryPatternRepeatEndDate = createDateTemplateForAnniversary(
			anniversaryPattern.repeatEndDate, ADD_DATE_FUNCTION, dayDiff
		);

		JPAUpdateClause anniversaryUpdateQuery = queryFactory.update(anniversary)
			.set(anniversary.date, dateTemplateForAnniversary)
			.where(anniversary.anniversaryPattern.id.in(anniversaryPatternIds));

		JPAUpdateClause anniversaryPatternStartDateUpdateQuery = queryFactory.update(
				anniversaryPattern)
			.set(anniversaryPattern.repeatStartDate,
				dateTemplateForAnniversaryPatternRepeatStartDate)
			.where(anniversaryPattern.id.in(anniversaryPatternIds));

		JPAUpdateClause anniversaryPatternEndDateUpdateQuery = queryFactory.update(
				anniversaryPattern)
			.set(anniversaryPattern.repeatEndDate, dateTemplateForAnniversaryPatternRepeatEndDate)
			.where(anniversaryPattern.id.in(anniversaryPatternIds));

		anniversaryUpdateQuery.execute();
		anniversaryPatternStartDateUpdateQuery.execute();
		anniversaryPatternEndDateUpdateQuery.execute();
	}

	public boolean existsByIdAndCoupleId(Long anniversaryId, Long coupleId) {

		Anniversary findAnniversary = queryFactory.selectFrom(anniversary)
			.innerJoin(anniversary.anniversaryPattern, anniversaryPattern)
			.where(anniversary.id.eq(anniversaryId), anniversaryPattern.couple.id.eq(coupleId))
			.fetchFirst();

		return findAnniversary != null;
	}

	private BooleanExpression startDateGoe(LocalDate startDate) {

		if (startDate == null) {
			return anniversary.date.goe(LocalDate.now());
		}

		return anniversary.date.goe(startDate);
	}

	private BooleanExpression yearEq(Integer year) {

		if (year == null) {
			return null;
		}

		return anniversary.date.year().eq(year);
	}

	private BooleanExpression monthEq(Integer month) {

		if (month == null) {
			return null;
		}

		return anniversary.date.month().eq(month);
	}

	private BooleanExpression dayEq(Integer day) {

		if (day == null) {
			return null;
		}

		return anniversary.date.dayOfMonth().eq(day);
	}

	private BooleanExpression coupleIdEq(Long coupleId) {

		return couple.id.eq(coupleId);
	}

	private DateTemplate<LocalDate> createDateTemplateForAnniversary(DatePath<LocalDate> datePath,
		String template, long dayDiff) {

		return Expressions.dateTemplate(
			LocalDate.class, template, datePath, Expressions.asNumber(dayDiff));
	}
}
