package com.dateplan.dateplan.service.anniversary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryCategory;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryPatternRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryRepository;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryReadService;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryDatesServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryListServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.ComingAnniversaryListServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.ComingAnniversaryServiceResponse;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.anniversary.AnniversaryNotFoundException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class AnniversaryReadServiceTest extends ServiceTestSupport {

	@Autowired
	private AnniversaryReadService anniversaryReadService;

	@MockBean
	private CoupleReadService coupleReadService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CoupleRepository coupleRepository;

	@Autowired
	private AnniversaryRepository anniversaryRepository;

	@Autowired
	private AnniversaryPatternRepository anniversaryPatternRepository;

	@Nested
	@DisplayName("기념일 날짜 조회시")
	class ReadAnniversaryDates {

		private static final String NEED_ANNIVERSARIES = "needAnniversaries";

		private Couple couple;
		private List<LocalDate> savedAnniversaryDates;

		@BeforeEach
		void setUp(TestInfo testInfo) {

			Member member = createMember("01011112222", "nickname1", LocalDate.of(1999, 10, 10));
			Member partner = createMember("01022223333", "nickname2", LocalDate.of(1999, 10, 10));

			memberRepository.saveAll(List.of(member, partner));

			couple = createCouple(member, partner, LocalDate.of(2020, 10, 10));
			coupleRepository.save(couple);

			if (testInfo.getTags().contains(NEED_ANNIVERSARIES)) {
				savedAnniversaryDates = List.of(
					LocalDate.of(2023, 2, 1),
					LocalDate.of(2023, 10, 10),
					LocalDate.of(2023, 1, 1),
					LocalDate.of(2023, 1, 2),
					LocalDate.of(2023, 1, 2));

				savedAnniversaryDates.forEach(date -> {
					AnniversaryPattern anniversaryPattern = createAnniversaryPattern(couple, date);
					Anniversary anniversary = createAnniversary("title", date, anniversaryPattern);
					anniversaryRepository.save(anniversary);
				});
			}
		}

		@AfterEach
		void tearDown(TestInfo testInfo) {

			if (testInfo.getTags().contains(NEED_ANNIVERSARIES)) {
				anniversaryRepository.deleteAllInBatch();
				anniversaryPatternRepository.deleteAllInBatch();
			}
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Tag(NEED_ANNIVERSARIES)
		@DisplayName("대상 커플의 id 와 현재 로그인 회원의 커플 id 가 같다면, 해당 연, 월에 맞는 기념일 날짜 목록을 중복 없이, 시간 순으로 반환한다.")
		@Test
		void withSameValueLoginMemberCoupleIdAndTargetCoupleId() {

			// given
			Long coupleId = couple.getId();
			Integer year = 2023;
			int month = 1;

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// when
			AnniversaryDatesServiceResponse serviceResponse = anniversaryReadService.readAnniversaryDates(coupleId, year, month);

			// then
			List<LocalDate> actualDates = serviceResponse.getAnniversaryDates();
			List<LocalDate> expectedDates = savedAnniversaryDates.stream()
				.filter(localDate -> localDate.getMonthValue() == month)
				.distinct()
				.toList();

			assertThat(actualDates)
				.containsExactlyElementsOf(expectedDates)
				.isSortedAccordingTo(LocalDate::compareTo);
		}
	}

	@Nested
	@DisplayName("기념일 조회시")
	class ReadAnniversaries {

		private static final String NEED_ANNIVERSARIES = "needAnniversaries";

		private Couple couple;
		private List<Anniversary> savedAnniversaries;

		@BeforeEach
		void setUp(TestInfo testInfo) {

			Member member = createMember("01011112222", "nickname1", LocalDate.of(1999, 10, 10));
			Member partner = createMember("01022223333", "nickname2", LocalDate.of(1999, 10, 10));

			memberRepository.saveAll(List.of(member, partner));

			couple = createCouple(member, partner, LocalDate.of(2020, 10, 10));
			coupleRepository.save(couple);

			if (testInfo.getTags().contains(NEED_ANNIVERSARIES)) {
				List<LocalDate> anniversaryDates = List.of(
					LocalDate.of(2023, 2, 1),
					LocalDate.of(2023, 1, 1),
					LocalDate.of(2023, 1, 1),
					LocalDate.of(2023, 1, 2));

				savedAnniversaries = anniversaryDates.stream()
					.map(date -> {
						AnniversaryPattern anniversaryPattern = createAnniversaryPattern(couple,
							date);
						Anniversary anniversary = createAnniversary("title", date,
							anniversaryPattern);
						return anniversary;
					}).toList();

				anniversaryRepository.saveAll(savedAnniversaries);
			}
		}

		@AfterEach
		void tearDown() {

			anniversaryRepository.deleteAllInBatch();
			anniversaryPatternRepository.deleteAllInBatch();

			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Tag(NEED_ANNIVERSARIES)
		@DisplayName("대상 커플의 id 와 현재 로그인 회원의 커플 id 가 같다면, 해당 연, 월, 일에 맞는 기념일 목록을 시간 순으로 반환한다.")
		@Test
		void withSameValueLoginMemberCoupleIdAndTargetCoupleId() {

			// given
			Long coupleId = couple.getId();
			int year = 2023;
			int month = 1;
			int day = 1;
			LocalDate date = LocalDate.of(year, month, day);

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// when
			AnniversaryListServiceResponse serviceResponse = anniversaryReadService.readAnniversaries(coupleId, year, month, day, false);

			// then
			List<AnniversaryServiceResponse> actualServiceResponseList = serviceResponse.getAnniversaries();
			List<AnniversaryServiceResponse> expectedServiceResponseList = savedAnniversaries.stream()
				.filter(anniversary -> Objects.equals(anniversary.getDate(), date))
				.map(AnniversaryServiceResponse::of)
				.toList();

			assertThat(actualServiceResponseList)
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(expectedServiceResponseList);
		}

		@DisplayName("대상 커플의 id 와 현재 로그인 회원의 커플 id 가 같고, 반복 기념일은 시작일만 조회한다면 해당 기념일 목록을 시간 순으로 반환한다.")
		@Test
		void withSameValueLoginMemberCoupleIdAndTargetCoupleIdAndReadWithOnlyReadRepeatStarted() {

			// given
			Long coupleId = couple.getId();
			LocalDate repeatStartDate = LocalDate.of(2020, 10, 10);
			LocalDate repeatEndDate = LocalDate.of(2021, 10, 10);
			AnniversaryPattern anniversaryPattern = AnniversaryPattern.builder()
				.couple(couple)
				.repeatStartDate(repeatStartDate)
				.repeatEndDate(repeatEndDate)
				.repeatRule(AnniversaryRepeatRule.YEAR)
				.category(AnniversaryCategory.OTHER)
				.build();

			Anniversary anniversary1 = createAnniversary("title1", repeatStartDate,
				anniversaryPattern);
			Anniversary anniversary2 = createAnniversary("title2", repeatEndDate,
				anniversaryPattern);

			anniversaryRepository.saveAll(List.of(anniversary1, anniversary2));

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// when
			AnniversaryListServiceResponse serviceResponse = anniversaryReadService.readAnniversaries(
				coupleId, null, null, null, true);

			// then
			List<AnniversaryServiceResponse> actualServiceResponseList = serviceResponse.getAnniversaries();
			List<AnniversaryServiceResponse> expectedServiceResponseList = List.of(
				AnniversaryServiceResponse.of(anniversary1));

			assertThat(actualServiceResponseList)
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(expectedServiceResponseList);
		}
	}

	@Nested
	@DisplayName("다가오는 기념일 조회시")
	class ReadComingAnniversaries {

		private static final String NEED_ANNIVERSARIES = "needAnniversaries";

		private Couple couple;
		private List<Anniversary> savedAnniversaries;

		@BeforeEach
		void setUp(TestInfo testInfo) {

			Member member = createMember("01011112222", "nickname1", LocalDate.of(1999, 10, 10));
			Member partner = createMember("01022223333", "nickname2", LocalDate.of(1999, 10, 10));

			memberRepository.saveAll(List.of(member, partner));

			couple = createCouple(member, partner, LocalDate.of(2020, 10, 10));
			coupleRepository.save(couple);

			if (testInfo.getTags().contains(NEED_ANNIVERSARIES)) {
				List<LocalDate> anniversaryDates = List.of(
					LocalDate.now().minusDays(1),
					LocalDate.now(),
					LocalDate.now().plusDays(1),
					LocalDate.now().plusDays(2));

				savedAnniversaries = anniversaryDates.stream()
					.map(date -> {
						AnniversaryPattern anniversaryPattern = createAnniversaryPattern(couple,
							date);
						Anniversary anniversary = createAnniversary("title", date,
							anniversaryPattern);
						return anniversary;
					}).toList();

				anniversaryRepository.saveAll(savedAnniversaries);
			}
		}

		@AfterEach
		void tearDown(TestInfo testInfo) {

			if (testInfo.getTags().contains(NEED_ANNIVERSARIES)) {
				anniversaryRepository.deleteAllInBatch();
				anniversaryPatternRepository.deleteAllInBatch();
			}
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Tag(NEED_ANNIVERSARIES)
		@DisplayName("대상 커플의 id 와 현재 로그인 회원의 커플 id 가 같다면, 해당 날짜를 포함한 이후의 기념일을 size 만큼 시간 순으로 반환한다.")
		@Test
		void withSameValueLoginMemberCoupleIdAndTargetCoupleId() {

			// given
			Long coupleId = couple.getId();
			LocalDate startDate = LocalDate.now();
			int size = 3;

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// when
			ComingAnniversaryListServiceResponse serviceResponse = anniversaryReadService.readComingAnniversaries(
				coupleId, startDate, size);

			// then
			List<ComingAnniversaryServiceResponse> actualServiceResponseList = serviceResponse.getAnniversaries();
			List<ComingAnniversaryServiceResponse> expectedServiceResponseList = savedAnniversaries.stream()
				.filter(anniversary -> Objects.equals(anniversary.getDate(), startDate)
					|| anniversary.getDate().isAfter(startDate))
				.map(ComingAnniversaryServiceResponse::from)
				.toList();

			assertThat(actualServiceResponseList)
				.hasSize(size)
				.isSortedAccordingTo((a1, a2) -> a1.getDate().isAfter(a2.getDate()) ? 1 : 0)
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(expectedServiceResponseList);
		}
	}

	@Nested
	@DisplayName("기념일 id 로 기념일 조회시")
	class FindAnniversaryByIdOrElseThrow {

		private static final String NEED_ANNIVERSARIES = "needAnniversaries";

		private Anniversary savedAnniversary;

		@BeforeEach
		void setUp(TestInfo testInfo) {

			if (testInfo.getTags().contains(NEED_ANNIVERSARIES)) {
				Member member = createMember("01011112222", "nickname1",
					LocalDate.of(1999, 10, 10));
				Member partner = createMember("01022223333", "nickname2",
					LocalDate.of(1999, 10, 10));

				memberRepository.saveAll(List.of(member, partner));

				Couple couple = createCouple(member, partner, LocalDate.of(2020, 10, 10));
				coupleRepository.save(couple);

				AnniversaryPattern anniversaryPattern = createAnniversaryPattern(couple,
					LocalDate.of(2022, 12, 12));

				savedAnniversary = createAnniversary("title", LocalDate.of(2020, 12, 12),
					anniversaryPattern);

				anniversaryRepository.save(savedAnniversary);
			}
		}

		@AfterEach
		void tearDown(TestInfo testInfo) {

			savedAnniversary = null;

			if (testInfo.getTags().contains(NEED_ANNIVERSARIES)) {
				anniversaryRepository.deleteAllInBatch();
				anniversaryPatternRepository.deleteAllInBatch();
				coupleRepository.deleteAllInBatch();
				memberRepository.deleteAllInBatch();
			}
		}

		@Tag(NEED_ANNIVERSARIES)
		@DisplayName("존재하는 id 로 조회한다면 기념일을 반환한다.")
		@Test
		void withExistsId() {

			// Given
			Long targetId = savedAnniversary.getId();

			// When
			Anniversary findAnniversary = anniversaryReadService.findAnniversaryByIdOrElseThrow(
				targetId, false);

			// Then
			assertThat(findAnniversary)
				.usingRecursiveComparison()
				.ignoringFields("anniversaryPattern")
				.isEqualTo(savedAnniversary);
		}

		@DisplayName("존재하지 않는 id 로 조회한다면, 예외를 발생시킨다.")
		@Test
		void withNotExistsId() {

			// given
			Long targetId = 1000L;

			AnniversaryNotFoundException expectedException = new AnniversaryNotFoundException();

			// when & then
			assertThatThrownBy(
				() -> anniversaryReadService.findAnniversaryByIdOrElseThrow(targetId, false))
				.isInstanceOf(expectedException.getClass())
				.hasMessage(expectedException.getMessage());
		}
	}

	private Anniversary createAnniversary(String title, LocalDate date,
		AnniversaryPattern anniversaryPattern) {

		return Anniversary.builder()
			.title(title)
			.date(date)
			.anniversaryPattern(anniversaryPattern)
			.build();
	}

	private AnniversaryPattern createAnniversaryPattern(Couple couple, LocalDate date) {

		return AnniversaryPattern.builder()
			.couple(couple)
			.repeatStartDate(date)
			.repeatEndDate(date)
			.repeatRule(AnniversaryRepeatRule.NONE)
			.category(AnniversaryCategory.OTHER)
			.build();
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
}
