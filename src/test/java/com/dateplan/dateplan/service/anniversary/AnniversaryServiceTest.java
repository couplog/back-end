package com.dateplan.dateplan.service.anniversary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import java.util.Objects;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
	private CoupleReadService coupleReadService;

	@Nested
	@DisplayName("생일 기념일을 생성시")
	class CreateAnniversaryForBirthDay {

		private Member connectedMember1;
		private Couple couple;

		@BeforeEach
		void setUp() {
			connectedMember1 = createMember("01011112222", "nickname1", LocalDate.of(2000, 1, 1));
			Member connectedMember2 = createMember("01022223333", "nickname2", LocalDate.of(2000, 1, 1));
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
					DateConstants.CALENDER_END_DATE.toString(),
					AnniversaryRepeatRule.YEAR.name());

			List<Anniversary> anniversaries = anniversaryRepository.findAll()
				.stream()
				.sorted((anniversary1, anniversary2) ->
					anniversary1.getDate().isAfter(anniversary2.getDate()) ? 1 : 0)
				.toList();

			LocalDate expectedDate = birthDay;

			for(Anniversary actual : anniversaries) {

				assertThat(actual.getCategory())
					.isEqualTo(AnniversaryCategory.BIRTH);
				assertThat(actual.getDate())
					.isEqualTo(expectedDate)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE);

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
			assertThatThrownBy(() -> anniversaryService.createAnniversariesForBirthDay(connectedMember1))
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

			assertThat(anniversaryPatterns)
				.filteredOn(anniversaryPattern -> anniversaryPattern.getRepeatRule().equals(AnniversaryRepeatRule.NONE))
				.hasSize(1);
			assertThat(anniversaryPatterns)
				.filteredOn(anniversaryPattern -> anniversaryPattern.getRepeatRule().equals(AnniversaryRepeatRule.YEAR))
				.hasSize(1);
			assertThat(anniversaryPatterns)
				.filteredOn(anniversaryPattern -> anniversaryPattern.getRepeatRule().equals(AnniversaryRepeatRule.HUNDRED_DAYS))
				.hasSize(1);

			List<Anniversary> anniversaries = anniversaryRepository.findAll();

			List<Anniversary> firstDateAnniversary = anniversaries.stream()
				.filter(anniversary -> Objects.equals(anniversary.getTitle(), "처음 만난 날"))
				.toList();

			assertThat(firstDateAnniversary)
				.hasSize(1)
				.extracting(
					Anniversary::getDate,
					Anniversary::getCategory
				).containsExactly(
					tuple(couple.getFirstDate(), AnniversaryCategory.FIRST_DATE)
				);

			List<Anniversary> firstDateAnniversaryWithYearRepeated = anniversaries.stream()
				.filter(anniversary -> anniversary.getTitle() != null && anniversary.getTitle().contains("주년"))
				.sorted((anniversary1, anniversary2) ->
					anniversary1.getDate().isAfter(anniversary2.getDate()) ? 1 : 0)
				.toList();

			LocalDate expectedDate1 = couple.getFirstDate().plusYears(1);

			for(Anniversary actual : firstDateAnniversaryWithYearRepeated) {

				assertThat(actual.getCategory())
					.isEqualTo(AnniversaryCategory.FIRST_DATE);
				assertThat(actual.getDate())
					.isEqualTo(expectedDate1)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE);

				expectedDate1 = expectedDate1.plusYears(1);
			}

			List<Anniversary> firstDateAnniversaryWithHundredDayRepeated = anniversaries.stream()
				.filter(anniversary -> anniversary.getTitle() != null && anniversary.getTitle().contains("일"))
				.sorted((anniversary1, anniversary2) ->
					anniversary1.getDate().isAfter(anniversary2.getDate()) ? 1 : 0)
				.toList();

			LocalDate expectedDate2 = couple.getFirstDate().plusDays(99);

			for(Anniversary actual : firstDateAnniversaryWithHundredDayRepeated) {

				assertThat(actual.getCategory())
					.isEqualTo(AnniversaryCategory.FIRST_DATE);
				assertThat(actual.getDate())
					.isEqualTo(expectedDate2)
					.isBefore(DateConstants.NEXT_DAY_FROM_CALENDER_END_DATE);

				expectedDate2 = expectedDate2.plusDays(100);
			}
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
			.firstDate(LocalDate.of(2010, 10, 10))
			.build();
	}
}
