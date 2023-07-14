package com.dateplan.dateplan.service.anniversary;

import static com.dateplan.dateplan.global.constant.DateConstants.CALENDER_END_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryCategory;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryJDBCRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryPatternRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryRepository;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryReadService;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryService;
import com.dateplan.dateplan.domain.anniversary.service.dto.request.AnniversaryCreateServiceRequest;
import com.dateplan.dateplan.domain.anniversary.service.dto.request.AnniversaryModifyServiceRequest;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.global.constant.DateConstants;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.anniversary.AnniversaryNotFoundException;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;

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
	private AnniversaryReadService anniversaryReadService;

	@MockBean
	private CoupleReadService coupleReadService;

	@MockBean
	private MemberReadService memberReadService;

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
			given(memberReadService.findMemberByIdOrElseThrow(anyLong()))
				.willReturn(connectedMember1);
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// When
			anniversaryService.createAnniversariesForBirthDay(connectedMember1.getId());

			// Then
			AnniversaryPattern anniversaryPattern = anniversaryPatternRepository.findAll().get(0);

			assertThat(anniversaryPattern)
				.extracting(
					AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					AnniversaryPattern::getRepeatRule,
					AnniversaryPattern::getCategory)
				.containsExactly(
					birthDay,
					CALENDER_END_DATE,
					AnniversaryRepeatRule.YEAR,
					AnniversaryCategory.BIRTH);

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
				assertThat(actual.getDate())
					.isEqualTo(expectedDate)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE);
				assertThat(actual.getAnniversaryPattern().getId())
					.isEqualTo(anniversaryPattern.getId());

				expectedDate = expectedDate.plusYears(1);
			}
		}

		@DisplayName("존재하지 않은 회원의 경우, 예외를 발생시킨다.")
		@Test
		void withNotExistsMember() {

			// Given
			Long memberId = connectedMember1.getId() + 100;

			// Stub
			MemberNotFoundException expectedException = new MemberNotFoundException();
			given(memberReadService.findMemberByIdOrElseThrow(anyLong()))
				.willThrow(expectedException);

			// when & then
			assertThatThrownBy(
				() -> anniversaryService.createAnniversariesForBirthDay(memberId))
				.isInstanceOf(expectedException.getClass())
				.hasMessage(expectedException.getMessage());

			then(anniversaryRepository)
				.shouldHaveNoInteractions();
			then(anniversaryPatternRepository)
				.shouldHaveNoInteractions();
			then(anniversaryJDBCRepository)
				.shouldHaveNoInteractions();
		}

		@DisplayName("연결되지 않은 회원의 경우, 예외를 발생시킨다.")
		@Test
		void withNotConnectedMember() {

			// Given
			Long memberId = connectedMember1.getId();

			// Stub
			given(memberReadService.findMemberByIdOrElseThrow(anyLong()))
				.willReturn(connectedMember1);

			MemberNotConnectedException expectedException = new MemberNotConnectedException();
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willThrow(expectedException);

			// when & then
			assertThatThrownBy(
				() -> anniversaryService.createAnniversariesForBirthDay(memberId))
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

			// given
			Long coupleId = couple.getId();

			// stub
			given(coupleReadService.findCoupleByIdOrElseThrow(anyLong()))
				.willReturn(couple);

			// when
			anniversaryService.createAnniversariesForFirstDate(coupleId);

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

			assertThat(noRepeatedAnniversaryPatterns)
				.hasSize(1)
				.extracting(AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					AnniversaryPattern::getCategory,
					anniversaryPattern -> anniversaryPattern.getCouple().getId())
				.containsExactly(
					tuple(firstDate,
						firstDate,
						AnniversaryCategory.FIRST_DATE,
						coupleId));
			assertThat(yearRepeatedAnniversaryPatterns)
				.hasSize(1)
				.extracting(AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					AnniversaryPattern::getCategory,
					anniversaryPattern -> anniversaryPattern.getCouple().getId())
				.containsExactly(
					tuple(firstDate,
						CALENDER_END_DATE,
						AnniversaryCategory.FIRST_DATE,
						coupleId));
			assertThat(hundredDaysRepeatedAnniversaryPatterns)
				.hasSize(1)
				.extracting(AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					AnniversaryPattern::getCategory,
					anniversaryPattern -> anniversaryPattern.getCouple().getId())
				.containsExactly(
					tuple(firstDate,
						CALENDER_END_DATE,
						AnniversaryCategory.FIRST_DATE,
						coupleId));

			Map<Long, List<Anniversary>> anniversaryMap = anniversaryRepository.findAll().stream()
				.collect(Collectors.groupingBy(
					anniversary -> anniversary.getAnniversaryPattern().getId()));

			Long noRepeatedAnniversaryPatternId = noRepeatedAnniversaryPatterns.get(0).getId();
			List<Anniversary> firstDateAnniversary = anniversaryMap.get(
				noRepeatedAnniversaryPatternId);

			assertThat(firstDateAnniversary)
				.hasSize(1)
				.allMatch(anniversary -> anniversary.getTitle().contains("처음만난날"))
				.extracting(
					Anniversary::getContent,
					Anniversary::getDate,
					anniversary -> anniversary.getAnniversaryPattern().getId()
				).containsExactly(
					tuple(null, couple.getFirstDate(),
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
			given(coupleReadService.findCoupleByIdOrElseThrow(anyLong()))
				.willReturn(couple);

			// When
			anniversaryService.createAnniversaries(couple.getId(), request);

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
					anniversary -> anniversary.getAnniversaryPattern().getId())
				.contains(
					tuple(title,
						date,
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
			given(coupleReadService.findCoupleByIdOrElseThrow(anyLong()))
				.willReturn(couple);

			// When
			anniversaryService.createAnniversaries(couple.getId(), request);

			// Then
			AnniversaryPattern anniversaryPattern = anniversaryPatternRepository.findAll().get(0);

			assertThat(anniversaryPattern)
				.extracting(
					AnniversaryPattern::getRepeatStartDate,
					AnniversaryPattern::getRepeatEndDate,
					AnniversaryPattern::getRepeatRule,
					AnniversaryPattern::getCategory)
				.containsExactly(
					date,
					CALENDER_END_DATE,
					AnniversaryRepeatRule.YEAR,
					AnniversaryCategory.OTHER);

			List<Anniversary> anniversaries = anniversaryRepository.findAll();

			LocalDate expectedDate = date;
			String expectedContent = request.getContent();
			Long anniversaryPatternId = anniversaryPattern.getId();

			for (Anniversary actual : anniversaries) {

				assertThat(actual.getTitle())
					.isEqualTo(title);
				assertThat(actual.getContent())
					.isEqualTo(expectedContent);
				assertThat(actual.getDate())
					.isEqualTo(expectedDate)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE);
				assertThat(actual.getAnniversaryPattern().getId())
					.isEqualTo(anniversaryPatternId);

				expectedDate = expectedDate.plusYears(1);
			}
		}
	}

	@Nested
	@DisplayName("일반 기념일 수정시")
	class ModifyAnniversary {

		private static final String NEED_ANNIVERSARY = "needAnniversary";

		private Couple couple;
		private List<Anniversary> savedAnniversaries;
		private LocalDate startDate;

		@BeforeEach
		void setUp(TestInfo testInfo) {

			Member member = createMember("01011112222", "nickname1", LocalDate.of(2000, 1, 1));
			Member partner = createMember("01022223333", "nickname2",
				LocalDate.of(2000, 1, 1));
			memberRepository.save(member);
			memberRepository.save(partner);

			couple = createCouple(member, partner,
				LocalDate.of(2010, 1, 1));
			coupleRepository.save(couple);

			if (testInfo.getTags().contains(NEED_ANNIVERSARY)) {

				startDate = LocalDate.of(2040, 10, 10);

				AnniversaryPattern anniversaryPattern = createAnniversaryPattern(couple,
					startDate, AnniversaryRepeatRule.YEAR);

				savedAnniversaries = createRepeatedAnniversaries(startDate,
					"title", "content", anniversaryPattern);

				anniversaryRepository.saveAll(savedAnniversaries);
			}
		}

		@AfterEach
		void tearDown() {

			savedAnniversaries = null;
			startDate = null;

			anniversaryRepository.deleteAllInBatch();
			anniversaryPatternRepository.deleteAllInBatch();

			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Tag(NEED_ANNIVERSARY)
		@DisplayName("로그인한 회원의 커플이 소유한 기념일이고 1년 반복일 때, 해당 기념일 및 연관된 기념일 모두 수정되고, 기념일 패턴도 함께 수정된다.")
		@Test
		void withLoginMembersCoupleHaveGivenYearRepeatedAnniversary() {

			// given
			Long targetAnniversaryId = savedAnniversaries.get(0).getId();
			String modifiedTitle = "newTitle";
			String modifiedContent = "newContent";
			int dayDiff = 20;
			LocalDate modifiedDate = startDate.plusDays(dayDiff);

			AnniversaryModifyServiceRequest serviceRequest = createAnniversaryModifyServiceRequest(
				modifiedTitle, modifiedContent, modifiedDate);

			List<LocalDate> originalLocalDates = savedAnniversaries.stream()
				.map(Anniversary::getDate)
				.sorted((a1, a2) -> a1.isAfter(a2) ? 1 : 0)
				.toList();

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);
			given(anniversaryReadService.findAnniversaryByIdOrElseThrow(anyLong(), anyBoolean()))
				.willReturn(savedAnniversaries.get(0));

			// when
			anniversaryService.modifyAnniversary(targetAnniversaryId, serviceRequest, false);

			// then
			savedAnniversaries = anniversaryRepository.findAllById(
				savedAnniversaries.stream().map(Anniversary::getId).toList());

			assertThat(savedAnniversaries)
				.allSatisfy(anniversary -> {
					assertThat(anniversary.getTitle()).isEqualTo(modifiedTitle);
					assertThat(anniversary.getContent()).isEqualTo(modifiedContent);
				});

			List<LocalDate> modifiedDates = savedAnniversaries.stream()
				.map(Anniversary::getDate)
				.sorted((a1, a2) -> a1.isAfter(a2) ? 1 : 0)
				.toList();

			int size = modifiedDates.size();
			for (int i = 0; i < size; i++) {
				assertThat(ChronoUnit.DAYS.between(originalLocalDates.get(i),
					modifiedDates.get(i))).isEqualTo(dayDiff);
			}

			Anniversary earliestAnniversary = savedAnniversaries.get(0);
			AnniversaryPattern savedAnniversaryPattern = anniversaryPatternRepository.findById(
				earliestAnniversary.getAnniversaryPattern().getId()).get();

			assertThat(savedAnniversaryPattern.getRepeatStartDate())
				.isEqualTo(earliestAnniversary.getDate());
		}

		@DisplayName("로그인한 회원의 커플이 소유한 기념일이고 반복 없는 기념일일 때, 해당 기념일이 수정되고, 기념일 패턴도 수정된다.")
		@Test
		void withLoginMembersCoupleHaveGivenNoneRepeatedAnniversary() {

			// given
			LocalDate startDate = LocalDate.of(2020, 10, 10);

			AnniversaryPattern anniversaryPattern = AnniversaryPattern.builder()
				.couple(couple)
				.repeatRule(AnniversaryRepeatRule.NONE)
				.category(AnniversaryCategory.OTHER)
				.repeatStartDate(startDate)
				.repeatEndDate(startDate)
				.build();

			Anniversary anniversary = Anniversary.builder()
				.title("title")
				.content("content")
				.anniversaryPattern(anniversaryPattern)
				.date(startDate)
				.build();

			anniversaryRepository.save(anniversary);

			String modifiedTitle = "newTitle";
			String modifiedContent = "newContent";
			int dayDiff = 20;
			LocalDate modifiedDate = startDate.plusDays(dayDiff);

			AnniversaryModifyServiceRequest serviceRequest = createAnniversaryModifyServiceRequest(
				modifiedTitle, modifiedContent, modifiedDate);

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);
			given(anniversaryReadService.findAnniversaryByIdOrElseThrow(anyLong(), anyBoolean()))
				.willReturn(anniversary);

			// when
			anniversaryService.modifyAnniversary(anniversary.getId(), serviceRequest, false);

			// then
			Anniversary findAnniversary = anniversaryRepository.findById(anniversary.getId()).get();

			assertThat(findAnniversary)
				.satisfies(actualAnniversary -> {
					assertThat(actualAnniversary.getTitle()).isEqualTo(modifiedTitle);
					assertThat(actualAnniversary.getContent()).isEqualTo(modifiedContent);
					assertThat(actualAnniversary.getDate()).isEqualTo(modifiedDate);
				});

			AnniversaryPattern findAnniversaryPattern = anniversaryPatternRepository.findById(
				anniversaryPattern.getId()).get();
			assertThat(findAnniversaryPattern)
				.satisfies(actualAnniversaryPattern -> {
					assertThat(actualAnniversaryPattern.getRepeatStartDate()).isEqualTo(
						modifiedDate);
					assertThat(actualAnniversaryPattern.getRepeatEndDate()).isEqualTo(modifiedDate);
				});
		}

		@DisplayName("존재하지 않는 기념일 id 가 주어졌다면, 예외를 발생시킨다.")
		@Test
		void withNotExistsAnniversaryId() {

			// given
			Long targetAnniversaryId = 1000L;
			AnniversaryModifyServiceRequest serviceRequest = createAnniversaryModifyServiceRequest(
				"title", "content", LocalDate.of(2020, 12, 12));

			AnniversaryNotFoundException expectedException = new AnniversaryNotFoundException();

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);
			given(anniversaryReadService.findAnniversaryByIdOrElseThrow(anyLong(), anyBoolean()))
				.willThrow(expectedException);

			// when & then
			assertThatThrownBy(() ->
				anniversaryService.modifyAnniversary(targetAnniversaryId, serviceRequest, false))
				.isInstanceOf(expectedException.getClass())
				.hasMessage(expectedException.getMessage());
		}

		@Tag(NEED_ANNIVERSARY)
		@DisplayName("생일 및 처음만난 날 관련 기념일이라면, 예외를 발생시킨다.")
		@EnumSource(value = AnniversaryCategory.class, mode = Mode.EXCLUDE, names = {"OTHER"})
		@ParameterizedTest
		void withFirstDateOrBirthDayAnniversary(AnniversaryCategory category) {

			// given
			Long targetAnniversaryId = savedAnniversaries.get(0).getId();

			AnniversaryModifyServiceRequest serviceRequest = createAnniversaryModifyServiceRequest(
				"title", "content", LocalDate.of(2020, 12, 12));

			NoPermissionException expectedException = new NoPermissionException(
				Resource.ANNIVERSARY, Operation.UPDATE);

			// stub
			Anniversary anniversary = savedAnniversaries.get(0);
			AnniversaryPattern anniversaryPattern = anniversary.getAnniversaryPattern();
			ReflectionTestUtils.setField(anniversaryPattern, "category", category);

			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);
			given(anniversaryReadService.findAnniversaryByIdOrElseThrow(anyLong(), anyBoolean()))
				.willReturn(anniversary);

			// when & then
			assertThatThrownBy(() ->
				anniversaryService.modifyAnniversary(targetAnniversaryId, serviceRequest, false))
				.isInstanceOf(expectedException.getClass())
				.hasMessage(expectedException.getMessage());
		}
	}

	@Nested
	@DisplayName("처음만난날 관련 기념일 수정시")
	class ModifyAnniversaryForFirstDate {

		private static final String NEED_ANNIVERSARY = "needAnniversary";

		private Couple couple;
		private LocalDate firstDate;

		@BeforeEach
		void setUp() {

			Member member = createMember("01011112222", "nickname1", LocalDate.of(2000, 1, 1));
			Member partner = createMember("01022223333", "nickname2", LocalDate.of(2000, 1, 1));
			memberRepository.saveAll(List.of(member, partner));

			firstDate = LocalDate.of(2010, 10, 10);

			couple = createCouple(member, partner, firstDate);
			coupleRepository.save(couple);

			AnniversaryPattern firstDatePattern = AnniversaryPattern.ofFirstDate(couple,
				firstDate, AnniversaryRepeatRule.NONE);
			AnniversaryPattern hundredDaysRepeatedPattern = AnniversaryPattern.ofFirstDate(couple,
				firstDate, AnniversaryRepeatRule.HUNDRED_DAYS);
			AnniversaryPattern yearRepeatedPattern = AnniversaryPattern.ofFirstDate(couple,
				firstDate, AnniversaryRepeatRule.YEAR);

			anniversaryPatternRepository.saveAll(
				List.of(firstDatePattern, hundredDaysRepeatedPattern, yearRepeatedPattern));

			List<Anniversary> firstDateAnniversaries = createRepeatedAnniversariesForFirstDate(
				firstDatePattern, firstDate);
			List<Anniversary> hundredDaysRepeatedAnniversaries = createRepeatedAnniversariesForFirstDate(
				hundredDaysRepeatedPattern, firstDate);
			List<Anniversary> yearRepeatedAnniversaries = createRepeatedAnniversariesForFirstDate(
				yearRepeatedPattern, firstDate);

			anniversaryJDBCRepository.saveAll(firstDateAnniversaries);
			anniversaryJDBCRepository.saveAll(hundredDaysRepeatedAnniversaries);
			anniversaryJDBCRepository.saveAll(yearRepeatedAnniversaries);
		}

		@AfterEach
		void tearDown() {

			couple = null;
			firstDate = null;

			anniversaryRepository.deleteAllInBatch();
			anniversaryPatternRepository.deleteAllInBatch();

			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Tag(NEED_ANNIVERSARY)
		@DisplayName("로그인한 회원의 커플이 소유한 기념일이고 1년 반복일 때, 해당 기념일 및 연관된 기념일 모두 수정되고, 기념일 패턴도 함께 수정된다.")
		@Test
		void withLoginMembersCoupleHaveGivenYearRepeatedAnniversary() {

			// given
			int dayDiff = 20;
			LocalDate modifiedDate = firstDate.plusDays(dayDiff);

			// when
			anniversaryService.modifyAnniversaryForFirstDate(couple.getId(), modifiedDate);

			// then
			List<AnniversaryPattern> changedPatterns = anniversaryPatternRepository.findAll();

			assertThat(changedPatterns)
				.isNotEmpty()
				.allMatch(anniversaryPattern -> Objects.equals(
					anniversaryPattern.getRepeatStartDate(), modifiedDate))
				.filteredOn(anniversaryPattern -> Objects.equals(anniversaryPattern.getRepeatRule(),
					AnniversaryRepeatRule.NONE))
				.allMatch(anniversaryPattern -> Objects.equals(
					anniversaryPattern.getRepeatEndDate(), modifiedDate
				));
		}
	}

	@Nested
	@DisplayName("기념일 삭제시")
	class DeleteAnniversary {

		private static final String NEED_ANNIVERSARY = "needAnniversary";

		private Couple couple;
		private List<Anniversary> savedAnniversaries;

		@BeforeEach
		void setUp(TestInfo testInfo) {

			Member member = createMember("01011112222", "nickname1", LocalDate.of(2000, 1, 1));
			Member partner = createMember("01022223333", "nickname2",
				LocalDate.of(2000, 1, 1));
			memberRepository.save(member);
			memberRepository.save(partner);

			couple = createCouple(member, partner,
				LocalDate.of(2010, 1, 1));
			coupleRepository.save(couple);

			if (testInfo.getTags().contains(NEED_ANNIVERSARY)) {

				LocalDate startDate = LocalDate.of(2040, 10, 10);

				AnniversaryPattern anniversaryPattern = createAnniversaryPattern(couple,
					startDate, AnniversaryRepeatRule.YEAR);

				savedAnniversaries = createRepeatedAnniversaries(startDate,
					"title", "content", anniversaryPattern);

				anniversaryRepository.saveAll(savedAnniversaries);
			}
		}

		@AfterEach
		void tearDown(TestInfo testInfo) {

			savedAnniversaries = null;

			if (testInfo.getTags().contains(NEED_ANNIVERSARY)) {
				anniversaryRepository.deleteAllInBatch();
				anniversaryPatternRepository.deleteAllInBatch();
			}
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Tag(NEED_ANNIVERSARY)
		@DisplayName("로그인한 회원의 커플이 소유한 기념일이라면, 해당 기념일 및 연관된 기념일 모두 삭제된다.")
		@Test
		void withLoginMembersCoupleHaveGivenAnniversary() {

			// given
			Long targetAnniversaryId = savedAnniversaries.get(0).getId();

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);
			given(anniversaryReadService.findAnniversaryByIdOrElseThrow(anyLong(), anyBoolean()))
				.willReturn(savedAnniversaries.get(0));

			// when
			anniversaryService.deleteAnniversary(targetAnniversaryId);

			// then
			List<Anniversary> actualAnniversaries = anniversaryRepository.findAll().stream()
				.filter(anniversary -> anniversary.getAnniversaryPattern().getCategory()
					.equals(AnniversaryCategory.OTHER))
				.toList();

			assertThat(actualAnniversaries)
				.isEmpty();
		}

		@DisplayName("존재하지 않는 기념일 id 가 주어졌다면, 예외를 발생시킨다.")
		@Test
		void withNotExistsAnniversaryId() {

			// given
			Long targetAnniversaryId = 1000L;

			AnniversaryNotFoundException expectedException = new AnniversaryNotFoundException();

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);
			given(anniversaryReadService.findAnniversaryByIdOrElseThrow(anyLong(), anyBoolean()))
				.willThrow(expectedException);

			// when & then
			assertThatThrownBy(() ->
				anniversaryService.deleteAnniversary(targetAnniversaryId))
				.isInstanceOf(expectedException.getClass())
				.hasMessage(expectedException.getMessage());
		}

		@Tag(NEED_ANNIVERSARY)
		@DisplayName("생일 및 처음만난 날 관련 기념일이라면, 예외를 발생시킨다.")
		@EnumSource(value = AnniversaryCategory.class, mode = Mode.EXCLUDE, names = {"OTHER"})
		@ParameterizedTest
		void withFirstDateOrBirthDayAnniversary(AnniversaryCategory category) {

			// given
			Long targetAnniversaryId = savedAnniversaries.get(0).getId();

			NoPermissionException expectedException = new NoPermissionException(
				Resource.ANNIVERSARY, Operation.UPDATE);

			// stub
			Anniversary anniversary = savedAnniversaries.get(0);
			AnniversaryPattern anniversaryPattern = anniversary.getAnniversaryPattern();
			ReflectionTestUtils.setField(anniversaryPattern, "category", category);

			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);
			given(anniversaryReadService.findAnniversaryByIdOrElseThrow(anyLong(), anyBoolean()))
				.willReturn(anniversary);

			// when & then
			assertThatThrownBy(() ->
				anniversaryService.deleteAnniversary(targetAnniversaryId))
				.isInstanceOf(expectedException.getClass())
				.hasMessage(expectedException.getMessage());
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

	private AnniversaryPattern createAnniversaryPattern(Couple couple, LocalDate startDate,
		AnniversaryRepeatRule repeatRule) {

		return AnniversaryPattern.builder()
			.repeatStartDate(startDate)
			.repeatEndDate(CALENDER_END_DATE)
			.repeatRule(repeatRule)
			.category(AnniversaryCategory.OTHER)
			.couple(couple)
			.build();
	}

	private List<Anniversary> createRepeatedAnniversaries(LocalDate startDate, String title,
		String content, AnniversaryPattern anniversaryPattern) {

		return IntStream.iterate(
				0,
				years -> startDate.plusYears(years)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE),
				years -> years + 1)
			.mapToObj(years -> Anniversary.builder()
				.title(title)
				.content(content)
				.date(startDate.plusYears(years))
				.anniversaryPattern(anniversaryPattern)
				.build()
			).toList();
	}

	private AnniversaryModifyServiceRequest createAnniversaryModifyServiceRequest(String title,
		String content, LocalDate date) {

		return AnniversaryModifyServiceRequest.builder()
			.title(title)
			.content(content)
			.date(date)
			.build();
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
					.mapToObj(
						days -> Anniversary.ofFirstDate(anniversaryPattern, anniversaryDate, days)
					).toList();
			}

			case YEAR -> IntStream.iterate(
					1,
					years -> firstDate.plusYears(years)
						.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE),
					years -> years + 1)
				.mapToObj(days -> Anniversary.ofFirstDate(anniversaryPattern, firstDate, days)
				).toList();

			case NONE -> List.of(Anniversary.ofFirstDate(anniversaryPattern, firstDate, 0));
		};
	}
}
