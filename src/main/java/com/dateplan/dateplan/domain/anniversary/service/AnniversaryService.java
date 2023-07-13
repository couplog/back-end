package com.dateplan.dateplan.domain.anniversary.service;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryCategory;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryJDBCRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryPatternRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryQueryRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryRepository;
import com.dateplan.dateplan.domain.anniversary.service.dto.request.AnniversaryCreateServiceRequest;
import com.dateplan.dateplan.domain.anniversary.service.dto.request.AnniversaryModifyServiceRequest;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.global.constant.DateConstants;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class AnniversaryService {

	private final MemberReadService memberReadService;
	private final CoupleReadService coupleReadService;
	private final AnniversaryReadService anniversaryReadService;
	private final AnniversaryPatternRepository anniversaryPatternRepository;
	private final AnniversaryRepository anniversaryRepository;
	private final AnniversaryJDBCRepository anniversaryJDBCRepository;
	private final AnniversaryQueryRepository anniversaryQueryRepository;

	public void createAnniversaries(Long coupleId,
		AnniversaryCreateServiceRequest request) {

		Couple couple = coupleReadService.findCoupleByIdOrElseThrow(coupleId);

		AnniversaryPattern anniversaryPattern = request.toAnniversaryPattern(couple);
		anniversaryPatternRepository.save(anniversaryPattern);

		List<Anniversary> anniversaries = createRepeatedAnniversaries(anniversaryPattern,
			request);

		anniversaryJDBCRepository.saveAll(anniversaries);
	}

	private List<Anniversary> createRepeatedAnniversaries(AnniversaryPattern anniversaryPattern,
		AnniversaryCreateServiceRequest request) {

		return switch (anniversaryPattern.getRepeatRule()) {

			case NONE -> List.of(
				Anniversary.builder()
					.title(request.getTitle())
					.content(request.getContent())
					.date(request.getDate())
					.anniversaryPattern(anniversaryPattern)
					.build());

			case YEAR -> {
				LocalDate date = request.getDate();
				String title = request.getTitle();
				String content = request.getContent();

				yield IntStream.iterate(
						0,
						years -> date.plusYears(years)
							.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE),
						years -> years + 1)
					.mapToObj(years ->
						Anniversary.builder()
							.title(title)
							.content(content)
							.date(date.plusYears(years))
							.anniversaryPattern(anniversaryPattern)
							.build())
					.toList();
			}
			case HUNDRED_DAYS -> List.of();
		};
	}

	public void createAnniversariesForBirthDay(Long memberId) {

		Member member = memberReadService.findMemberByIdOrElseThrow(memberId);

		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		LocalDate birthDay = member.getBirthDay();

		AnniversaryPattern anniversaryPattern = AnniversaryPattern.ofBirthDay(couple, birthDay);
		anniversaryPatternRepository.save(anniversaryPattern);

		List<Anniversary> anniversaries = createRepeatedAnniversariesForBirthDay(member, birthDay,
			anniversaryPattern);

		anniversaryJDBCRepository.saveAll(anniversaries);
	}

	private List<Anniversary> createRepeatedAnniversariesForBirthDay(Member member,
		LocalDate birthDay,
		AnniversaryPattern anniversaryPattern) {

		return IntStream.iterate(
				0,
				years -> birthDay.plusYears(years)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE),
				years -> years + 1)
			.mapToObj(years ->
				Anniversary.builder()
					.title(member.getName() + " 님의 생일")
					.date(birthDay.plusYears(years))
					.anniversaryPattern(anniversaryPattern)
					.build())
			.toList();
	}

	public void createAnniversariesForFirstDate(Long coupleId) {

		Couple couple = coupleReadService.findCoupleByIdOrElseThrow(coupleId);

		LocalDate firstDate = couple.getFirstDate();

		AnniversaryPattern anniversaryPattern = AnniversaryPattern.ofFirstDate(couple, firstDate,
			AnniversaryRepeatRule.NONE);

		anniversaryPatternRepository.save(anniversaryPattern);

		Anniversary anniversary = Anniversary.builder()
			.title("처음 만난 날")
			.date(firstDate)
			.anniversaryPattern(anniversaryPattern)
			.build();
		anniversaryRepository.save(anniversary);

		createAndSaveAnniversariesForFirstDate(couple, AnniversaryRepeatRule.HUNDRED_DAYS);
		createAndSaveAnniversariesForFirstDate(couple, AnniversaryRepeatRule.YEAR);
	}

	private void createAndSaveAnniversariesForFirstDate(Couple couple,
		AnniversaryRepeatRule repeatRule) {

		LocalDate firstDate = couple.getFirstDate();

		if (!Objects.equals(repeatRule, AnniversaryRepeatRule.NONE)) {
			AnniversaryPattern anniversaryPattern = AnniversaryPattern.ofFirstDate(couple,
				firstDate,
				repeatRule);

			anniversaryPatternRepository.save(anniversaryPattern);

			List<Anniversary> anniversaries = createRepeatedAnniversariesForFirstDate(
				anniversaryPattern, firstDate);

			anniversaryJDBCRepository.saveAll(anniversaries);
		}
	}

	private List<Anniversary> createRepeatedAnniversariesForFirstDate(
		AnniversaryPattern anniversaryPattern, LocalDate firstDate) {

		return switch (anniversaryPattern.getRepeatRule()) {

			case HUNDRED_DAYS -> {
				LocalDate anniversaryDate = firstDate.minusDays(1);

				yield IntStream.iterate(
						100,
						days -> anniversaryDate.plusDays(days)
							.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE),
						days -> days + 100)
					.mapToObj(days -> Anniversary.builder()
						.title("만난지 " + days + "일")
						.date(anniversaryDate.plusDays(days))
						.anniversaryPattern(anniversaryPattern)
						.build()
					).toList();
			}

			case YEAR -> IntStream.iterate(
					1,
					years -> firstDate.plusYears(years)
						.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE),
					years -> years + 1)
				.mapToObj(years -> Anniversary.builder()
					.title("만난지 " + years + "주년")
					.date(firstDate.plusYears(years))
					.anniversaryPattern(anniversaryPattern)
					.build()
				).toList();

			case NONE -> List.of();
		};
	}

	public void modifyAnniversary(Long anniversaryId, AnniversaryModifyServiceRequest request,
		boolean isInternalCall) {

		Anniversary anniversary = anniversaryReadService.findAnniversaryByIdOrElseThrow(
			anniversaryId, true);
		AnniversaryPattern anniversaryPattern = anniversary.getAnniversaryPattern();

		if (!isInternalCall && !Objects.equals(anniversaryPattern.getCategory(),
			AnniversaryCategory.OTHER)) {
			throw new NoPermissionException(Resource.ANNIVERSARY, Operation.UPDATE);
		}

		anniversaryQueryRepository.updateAllRepeatedAnniversary(anniversaryId, request.getTitle(),
			request.getContent(), request.getDate());

		long dayDiff = ChronoUnit.DAYS.between(anniversary.getDate(), request.getDate());
		anniversaryPattern.updateDates(dayDiff);
		anniversaryPatternRepository.save(anniversaryPattern);
	}

	public void modifyAnniversaryForFirstDate(Long coupleId, LocalDate changedDate){

		anniversaryQueryRepository.updateAllRepeatedAnniversaryForFirstDate(coupleId, changedDate);
	}

	public void deleteAnniversary(Long anniversaryId) {

		Anniversary anniversary = anniversaryReadService.findAnniversaryByIdOrElseThrow(
			anniversaryId, true);

		AnniversaryPattern anniversaryPattern = anniversary.getAnniversaryPattern();

		if (!Objects.equals(anniversaryPattern.getCategory(), AnniversaryCategory.OTHER)
		) {
			throw new NoPermissionException(Resource.ANNIVERSARY, Operation.UPDATE);
		}

		anniversaryRepository.deleteAllByAnniversaryPatternId(anniversaryPattern.getId());
	}

}
