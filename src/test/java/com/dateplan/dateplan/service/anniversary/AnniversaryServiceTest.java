package com.dateplan.dateplan.service.anniversary;

import static com.dateplan.dateplan.global.constant.DateConstants.CALENDER_END_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.dateplan.dateplan.domain.anniversary.dto.AnniversaryCreateServiceRequest;
import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryCategory;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryJDBCRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryPatternRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryRepository;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryService;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.constant.DateConstants;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

public class AnniversaryServiceTest extends ServiceTestSupport {

	@Autowired
	private AnniversaryService anniversaryService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CoupleRepository coupleRepository;

	@SpyBean
	private AnniversaryRepository anniversaryRepository;

	@SpyBean
	private AnniversaryPatternRepository anniversaryPatternRepository;

	@SpyBean
	private AnniversaryJDBCRepository anniversaryJDBCRepository;

	@MockBean
	private CoupleReadService coupleReadService;

	@Nested
	@DisplayName("생일 기념일을 생성시")
	class CreateAnniversaryForBirthDay {

		private Member connectedMember1;
		private Couple couple;

		@BeforeEach
		void setUp() {
			connectedMember1 = createMember("01011112222", "nickname1", LocalDate.of(2000, 1, 1));
			Member connectedMember2 = createMember("01022223333", "nickname2",
				LocalDate.of(2000, 1, 1));
			memberRepository.save(connectedMember1);
			memberRepository.save(connectedMember2);

			couple = createCouple(connectedMember1, connectedMember2,
				LocalDate.of(2010, 1, 1));
			coupleRepository.save(couple);
		}

		@AfterEach
		void tearDown() {
			anniversaryRepository.deleteAllInBatch();
			anniversaryPatternRepository.deleteAllInBatch();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("대상 회원의 생일을 기점으로 기념일 패턴이 생성되고, 1년 단위로 2050년 이전까지 기념일이 생성된다.")
		@Test
		void withConnectedMember() {

			// Given
			LocalDate birthDay = connectedMember1.getBirthDay();

			// Stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// When
			anniversaryService.createAnniversariesForBirthDay(connectedMember1);

			// Then
			AnniversaryPattern anniversaryPattern = anniversaryPatternRepository.findAll().get(0);

			assertThat(anniversaryPattern)
				.extracting(
					AnniversaryPattern -> AnniversaryPattern.getRepeatStartDate().toString(),
					AnniversaryPattern -> AnniversaryPattern.getRepeatEndDate().toString(),
					AnniversaryPattern -> AnniversaryPattern.getRepeatRule().name())
				.containsExactly(
					birthDay.toString(),
					CALENDER_END_DATE.toString(),
					AnniversaryRepeatRule.YEAR.name());

			List<Anniversary> anniversaries = anniversaryRepository.findAll()
				.stream()
				.sorted((anniversary1, anniversary2) ->
					anniversary1.getDate().isAfter(anniversary2.getDate()) ? 1 : 0)
				.toList();

			LocalDate expectedDate = birthDay;

			for (Anniversary actual : anniversaries) {

				assertThat(actual.getTitle())
					.contains(connectedMember1.getName(), "생일");
				assertThat(actual.getContent())
					.isNull();
				assertThat(actual.getCategory())
					.isEqualTo(AnniversaryCategory.BIRTH);
				assertThat(actual.getDate())
					.isEqualTo(expectedDate)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE);
				assertThat(actual.getAnniversaryPattern().getId())
					.isEqualTo(anniversaryPattern.getId());

				expectedDate = expectedDate.plusYears(1);
			}
		}

		@DisplayName("연결되지 않은 회원의 경우, 예외를 발생시킨다.")
		@Test
		void withNotConnectedMember() {

			// Stub
			MemberNotConnectedException expectedException = new MemberNotConnectedException();
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willThrow(expectedException);

			// when & then
			assertThatThrownBy(
				() -> anniversaryService.createAnniversariesForBirthDay(connectedMember1))
				.isInstanceOf(expectedException.getClass())
				.hasMessage(expectedException.getMessage());

			then(anniversaryRepository)
				.shouldHaveNoInteractions();
			then(anniversaryPatternRepository)
				.shouldHaveNoInteractions();
			then(anniversaryJDBCRepository)
				.shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("처음 만난 날 관련 기념일을 생성시")
	class CreateAnniversariesForFirstDate {

		private Couple couple;

		@BeforeEach
		void setUp() {
			Member connectedMember1 = createMember("01011112222", "nickname1",
				LocalDate.of(2000, 1, 1));
			Member connectedMember2 = createMember("01022223333", "nickname2",
				LocalDate.of(2000, 1, 1));
			memberRepository.save(connectedMember1);
			memberRepository.save(connectedMember2);

			couple = createCouple(connectedMember1, connectedMember2,
				LocalDate.of(2010, 1, 1));
			coupleRepository.save(couple);
		}

		@AfterEach
		void tearDown() {
			anniversaryRepository.deleteAllInBatch();
			anniversaryPatternRepository.deleteAllInBatch();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("대상 커플의 처음 만난 날 당일과 100일 주기, 1년 주기 기념일 패턴이 생성되고, 각 반복 주기에 맞게 2050년 이전까지 기념일이 생성된다.")
		@Test
		void withConnectedMember() {

			// when
			anniversaryService.createAnniversariesForFirstDate(couple);

			// then
			List<AnniversaryPattern> anniversaryPatterns = anniversaryPatternRepository.findAll();

			Map<AnniversaryRepeatRule, List<AnniversaryPattern>> anniversaryPatternMap = anniversaryPatterns.stream()
				.collect(Collectors.groupingBy(AnniversaryPattern::getRepeatRule));

			List<AnniversaryPattern> noRepeatedAnniversaryPatterns = anniversaryPatternMap.get(
				AnniversaryRepeatRule.NONE);
			List<AnniversaryPattern> yearRepeatedAnniversaryPatterns = anniversaryPatternMap.get(
				AnniversaryRepeatRule.YEAR);
			List<AnniversaryPattern> hundredDaysRepeatedAnniversaryPatterns = anniversaryPatternMap.get(
				AnniversaryRepeatRule.HUNDRED_DAYS);

			LocalDate firstDate = couple.getFirstDate();
			Long coupleId = couple.getId();

			assertThat(noRepeatedAnniversaryPatterns)
				.hasSize(1)
				.extracting(AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					anniversaryPattern -> anniversaryPattern.getCouple().getId())
				.containsExactly(
					tuple(firstDate,
						firstDate,
						coupleId));
			assertThat(yearRepeatedAnniversaryPatterns)
				.hasSize(1)
				.extracting(AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					anniversaryPattern -> anniversaryPattern.getCouple().getId())
				.containsExactly(
					tuple(firstDate,
						CALENDER_END_DATE,
						coupleId));
			assertThat(hundredDaysRepeatedAnniversaryPatterns)
				.hasSize(1)
				.extracting(AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					anniversaryPattern -> anniversaryPattern.getCouple().getId())
				.containsExactly(
					tuple(firstDate,
						CALENDER_END_DATE,
						coupleId));

			Map<Long, List<Anniversary>> anniversaryMap = anniversaryRepository.findAll().stream()
				.collect(Collectors.groupingBy(
					anniversary -> anniversary.getAnniversaryPattern().getId()));

			Long noRepeatedAnniversaryPatternId = noRepeatedAnniversaryPatterns.get(0).getId();
			List<Anniversary> firstDateAnniversary = anniversaryMap.get(
				noRepeatedAnniversaryPatternId);

			assertThat(firstDateAnniversary)
				.hasSize(1)
				.allMatch(anniversary -> anniversary.getTitle().contains("처음 만난 날"))
				.extracting(
					Anniversary::getContent,
					Anniversary::getDate,
					Anniversary::getCategory,
					anniversary -> anniversary.getAnniversaryPattern().getId()
				).containsExactly(
					tuple(null, couple.getFirstDate(), AnniversaryCategory.FIRST_DATE,
						noRepeatedAnniversaryPatternId)
				);

			Long yearRepeatedAnniversaryPatternId = yearRepeatedAnniversaryPatterns.get(0).getId();
			List<Anniversary> firstDateAnniversaryWithYearRepeated = anniversaryMap.get(
				yearRepeatedAnniversaryPatternId);

			LocalDate expectedDate1 = couple.getFirstDate().plusYears(1);

			for (Anniversary actual : firstDateAnniversaryWithYearRepeated) {

				assertThat(actual.getTitle())
					.contains("만난지", "주년");
				assertThat(actual.getContent())
					.isNull();
				assertThat(actual.getCategory())
					.isEqualTo(AnniversaryCategory.FIRST_DATE);
				assertThat(actual.getDate())
					.isEqualTo(expectedDate1)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE);
				assertThat(actual.getAnniversaryPattern().getId())
					.isEqualTo(yearRepeatedAnniversaryPatternId);

				expectedDate1 = expectedDate1.plusYears(1);
			}

			Long hundredDaysRepeatedAnniversaryPatternId = hundredDaysRepeatedAnniversaryPatterns.get(
				0).getId();
			List<Anniversary> firstDateAnniversaryWithHundredDayRepeated = anniversaryMap.get(
				hundredDaysRepeatedAnniversaryPatternId);

			LocalDate expectedDate2 = couple.getFirstDate().plusDays(99);

			for (Anniversary actual : firstDateAnniversaryWithHundredDayRepeated) {

				assertThat(actual.getTitle())
					.contains("만난지", "일");
				assertThat(actual.getContent())
					.isNull();
				assertThat(actual.getCategory())
					.isEqualTo(AnniversaryCategory.FIRST_DATE);
				assertThat(actual.getDate())
					.isEqualTo(expectedDate2)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE);
				assertThat(actual.getAnniversaryPattern().getId())
					.isEqualTo(hundredDaysRepeatedAnniversaryPatternId);

				expectedDate2 = expectedDate2.plusDays(100);
			}
		}
	}

	@Nested
	@DisplayName("일반 기념일 생성시")
	class CreateAnniversaries {

		private Member connectedMember1;
		private Couple couple;

		@BeforeEach
		void setUp() {
			connectedMember1 = createMember("01011112222", "nickname1", LocalDate.of(2000, 1, 1));
			Member connectedMember2 = createMember("01022223333", "nickname2",
				LocalDate.of(2000, 1, 1));
			memberRepository.save(connectedMember1);
			memberRepository.save(connectedMember2);

			couple = createCouple(connectedMember1, connectedMember2,
				LocalDate.of(2010, 1, 1));
			coupleRepository.save(couple);
		}

		@AfterEach
		void tearDown() {
			anniversaryRepository.deleteAllInBatch();
			anniversaryPatternRepository.deleteAllInBatch();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("기념일 반복이 없다면 기념일 패턴이 생성되고, 해당 날짜로 기념일이 생성된다.")
		@Test
		void withConnectedMemberAndNoRepeatedPattern() {

			// Given
			String title = "단일 기념일";
			LocalDate date = LocalDate.of(2023, 1, 26);
			AnniversaryRepeatRule repeatRule = AnniversaryRepeatRule.NONE;
			AnniversaryCreateServiceRequest request = createAnniversaryCreateServiceRequest(
				title, date, repeatRule);

			// Stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// When
			anniversaryService.createAnniversaries(connectedMember1, couple.getId(), request);

			// Then
			AnniversaryPattern anniversaryPattern = anniversaryPatternRepository.findAll().get(0);

			assertThat(anniversaryPattern)
				.extracting(
					AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					AnniversaryPattern::getRepeatRule)
				.containsExactly(
					date,
					date,
					AnniversaryRepeatRule.NONE);

			List<Anniversary> anniversaries = anniversaryRepository.findAll();

			assertThat(anniversaries)
				.hasSize(1)
				.extracting(Anniversary::getTitle,
					Anniversary::getDate,
					Anniversary::getCategory,
					anniversary -> anniversary.getAnniversaryPattern().getId())
				.contains(
					tuple(title,
						date,
						AnniversaryCategory.OTHER,
						anniversaryPattern.getId()));
		}

		@DisplayName("기념일 반복이 1년 주기라면 기념일 패턴이 생성되고, 1년 주기로 기념일이 생성된다.")
		@Test
		void withConnectedMemberAndYearRepeatedPattern() {

			// Given
			String title = "1년 반복 기념일";
			LocalDate date = LocalDate.of(2023, 1, 26);
			AnniversaryRepeatRule repeatRule = AnniversaryRepeatRule.YEAR;
			AnniversaryCreateServiceRequest request = createAnniversaryCreateServiceRequest(
				title, date, repeatRule);

			// Stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// When
			anniversaryService.createAnniversaries(connectedMember1, couple.getId(), request);

			// Then
			AnniversaryPattern anniversaryPattern = anniversaryPatternRepository.findAll().get(0);

			assertThat(anniversaryPattern)
				.extracting(
					AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					AnniversaryPattern::getRepeatRule)
				.containsExactly(
					date,
					CALENDER_END_DATE,
					AnniversaryRepeatRule.YEAR);

			List<Anniversary> anniversaries = anniversaryRepository.findAll();

			LocalDate expectedDate = date;
			String expectedContent = request.getContent();
			Long anniversaryPatternId = anniversaryPattern.getId();

			for (Anniversary actual : anniversaries) {

				assertThat(actual.getTitle())
					.isEqualTo(title);
				assertThat(actual.getContent())
					.isEqualTo(expectedContent);
				assertThat(actual.getCategory())
					.isEqualTo(AnniversaryCategory.OTHER);
				assertThat(actual.getDate())
					.isEqualTo(expectedDate)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE);
				assertThat(actual.getAnniversaryPattern().getId())
					.isEqualTo(anniversaryPatternId);

				expectedDate = expectedDate.plusYears(1);
			}
		}

		@DisplayName("연결되지 않은 회원의 경우, 예외를 발생시킨다.")
		@Test
		void withNotConnectedMember() {

			// Given
			String title = "기념일";
			LocalDate date = LocalDate.of(2023, 1, 26);
			AnniversaryRepeatRule repeatRule = AnniversaryRepeatRule.NONE;
			AnniversaryCreateServiceRequest request = createAnniversaryCreateServiceRequest(
				title, date, repeatRule);

			// Stub
			MemberNotConnectedException expectedException = new MemberNotConnectedException();
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willThrow(expectedException);

			// when & then
			assertThatThrownBy(
				() -> anniversaryService.createAnniversaries(connectedMember1, couple.getId(),
					request))
				.isInstanceOf(expectedException.getClass())
				.hasMessage(expectedException.getMessage());

			then(anniversaryRepository)
				.shouldHaveNoInteractions();
			then(anniversaryPatternRepository)
				.shouldHaveNoInteractions();
			then(anniversaryJDBCRepository)
				.shouldHaveNoInteractions();
		}
	}

	private Member createMember(String phone, String nickname, LocalDate birth) {

		return Member.builder()
			.name("홍길동")
			.nickname(nickname)
			.phone(phone)
			.password("password")
			.gender(Gender.FEMALE)
			.birthDay(birth)
			.build();
	}

	private Couple createCouple(Member member1, Member member2, LocalDate firstDate) {

		return Couple.builder()
			.member1(member1)
			.member2(member2)
			.firstDate(firstDate)
			.build();
	}

	private AnniversaryCreateServiceRequest createAnniversaryCreateServiceRequest(String title,
		LocalDate date, AnniversaryRepeatRule repeatRule) {

		return AnniversaryCreateServiceRequest.builder()
			.title(title)
			.content("내용")
			.date(date)
			.repeatRule(repeatRule)
			.build();
	}
}
