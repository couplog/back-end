package com.dateplan.dateplan.domain.anniversary.service;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryJDBCRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryPatternRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryRepository;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class AnniversaryService {

	private final CoupleReadService coupleReadService;
	private final AnniversaryPatternRepository anniversaryPatternRepository;
	private final AnniversaryRepository anniversaryRepository;
	private final AnniversaryJDBCRepository anniversaryJDBCRepository;

	public void createAnniversaryForBirthDay(Member member) {

		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		LocalDate birthDay = member.getBirthDay();

		AnniversaryPattern anniversaryPattern = AnniversaryPattern.builder()
			.couple(couple)
			.repeatStartDate(birthDay)
			.repeatEndDate(birthDay)
			.repeatRule(AnniversaryRepeatRule.YEAR)
			.build();

		anniversaryPatternRepository.save(anniversaryPattern);

		LocalDate limitEndDate = LocalDate.of(2050, 1, 1);

		List<Anniversary> anniversaries = IntStream.iterate(0,
				years -> birthDay.plusYears(years).isBefore(limitEndDate),
				years -> years + 1)
			.mapToObj(years -> Anniversary.ofBirthDay(
				member.getName() + " 님의 생일",
				birthDay.plusYears(years),
				anniversaryPattern
			))
			.toList();

		anniversaryJDBCRepository.saveAll(anniversaries);
	}

	public void createAnniversaryForFirstDate(Couple couple) {

		LocalDate firstDate = couple.getFirstDate();

		AnniversaryPattern anniversaryPattern = AnniversaryPattern.builder()
			.couple(couple)
			.repeatStartDate(firstDate)
			.repeatEndDate(firstDate)
			.repeatRule(AnniversaryRepeatRule.NONE)
			.build();

		anniversaryPatternRepository.save(anniversaryPattern);

		Anniversary anniversary = Anniversary.ofFirstDate("처음 만난 날", firstDate, anniversaryPattern);
		anniversaryRepository.save(anniversary);

		createAnniversaryForFirstDateWithHundredRepeat(couple);
		createAnniversaryForFirstDateWithYearRepeat(couple);
	}

	private void createAnniversaryForFirstDateWithHundredRepeat(Couple couple) {

		LocalDate firstDate = couple.getFirstDate();

		AnniversaryPattern anniversaryPattern = AnniversaryPattern.builder()
			.couple(couple)
			.repeatStartDate(firstDate)
			.repeatRule(AnniversaryRepeatRule.HUNDRED_DAYS)
			.build();

		anniversaryPatternRepository.save(anniversaryPattern);

		// 첫 만남 날짜는 1일로 간주
		LocalDate anniversaryDate = firstDate.minusDays(1L);
		LocalDate limitEndDate = LocalDate.of(2050, 1, 1);

		List<Anniversary> anniversaries = IntStream.iterate(100,
				days -> anniversaryDate.plusDays(days).isBefore(limitEndDate),
				days -> days + 100)
			.mapToObj(days -> Anniversary.ofFirstDate(
				"만난지 " + days + "일",
				anniversaryDate.plusDays(days),
				anniversaryPattern
			))
			.toList();

		anniversaryJDBCRepository.saveAll(anniversaries);
	}

	private void createAnniversaryForFirstDateWithYearRepeat(Couple couple) {

		LocalDate firstDate = couple.getFirstDate();

		AnniversaryPattern anniversaryPattern = AnniversaryPattern.builder()
			.couple(couple)
			.repeatStartDate(firstDate)
			.repeatRule(AnniversaryRepeatRule.YEAR)
			.build();

		anniversaryPatternRepository.save(anniversaryPattern);

		LocalDate limitEndDate = LocalDate.of(2050, 1, 1);

		List<Anniversary> anniversaries = IntStream.iterate(1,
				years -> firstDate.plusYears(years).isBefore(limitEndDate),
				years -> years + 1)
			.mapToObj(years -> Anniversary.ofFirstDate(
				"만난지 " + years + "주년",
				firstDate.plusYears(years),
				anniversaryPattern
			))
			.toList();

		anniversaryJDBCRepository.saveAll(anniversaries);
	}
}
