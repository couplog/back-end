package com.dateplan.dateplan.service.dating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.dating.controller.dto.response.DatingEntry;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.domain.dating.repository.DatingQueryRepository;
import com.dateplan.dateplan.domain.dating.repository.DatingRepository;
import com.dateplan.dateplan.domain.dating.service.DatingReadService;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingDatesServiceResponse;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.dating.DatingNotFoundException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

public class DatingReadServiceTest extends ServiceTestSupport {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CoupleRepository coupleRepository;

	@Autowired
	private DatingRepository datingRepository;

	@MockBean
	private CoupleReadService coupleReadService;

	@Autowired
	private DatingReadService datingReadService;

	@DisplayName("일정 날짜를 조회할 때")
	@Nested
	class ReadDatingDate {

		private static final String NEED_DATING = "needDating";

		private Couple couple;
		private List<LocalDate> savedDatingDates;

		@BeforeEach
		void setUp(TestInfo testInfo) {
			Member member = memberRepository.save(createMember("01012345678", "aaa"));
			Member partner = memberRepository.save(createMember("01012345679", "bbb"));
			couple = coupleRepository.save(createCouple(member, partner));
			MemberThreadLocal.set(member);

			if (testInfo.getTags().contains(NEED_DATING)) {
				LocalDate now = LocalDate.now();
				savedDatingDates = now.datesUntil(now.plusMonths(2)).toList();
				List<Dating> savedDatingList = savedDatingDates.stream()
					.map(date -> createDating(date, couple))
					.toList();
				datingRepository.saveAll(savedDatingList);

			}
		}

		@AfterEach
		void tearDown(TestInfo testInfo) {
			if (testInfo.getTags().contains(NEED_DATING)) {
				datingRepository.deleteAllInBatch();
			}
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
			MemberThreadLocal.remove();
		}

		@Tag(NEED_DATING)
		@DisplayName("올바른 coupleId를 요청하면 성공한다")
		@Test
		void successWithValidRequest() {

			// Stubbing
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// Given & When
			DatingDatesServiceResponse response = datingReadService.readDatingDates(
				couple.getId(), null, null);

			// Then
			List<LocalDate> actualDates = response.getDatingDates();
			assertThat(actualDates)
				.containsExactlyElementsOf(savedDatingDates)
				.isSortedAccordingTo(LocalDate::compareTo);
		}

		@Tag(NEED_DATING)
		@DisplayName("올바른 coupleId, 특정한 연도와 월을 요청하면 그에 해당하는 일정만 조회된다.")
		@Test
		void successWithValidRequestAndParam() {

			// Given
			Integer year = LocalDate.now().getYear();
			Integer month = LocalDate.now().getMonthValue();

			// Stubbing
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// When
			DatingDatesServiceResponse response = datingReadService.readDatingDates(
				couple.getId(), year, month);

			// Then
			List<LocalDate> actualDates = response.getDatingDates();

			actualDates.forEach(
				date -> {
					assertThat(date.getYear()).isEqualTo(year);
					assertThat(date.getMonthValue()).isEqualTo(month);
				}
			);
			assertThat(actualDates)
				.isSortedAccordingTo(LocalDate::compareTo);
		}
	}

	@Nested
	@DisplayName("데이트 일정을 조회할 때")
	class ReadDating {

		private Member member;
		private Couple couple;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("01012345678", "aaa"));
			Member partner = memberRepository.save(createMember("01012345679", "bbb"));

			couple = coupleRepository.save(
				Couple.builder()
					.member1(member)
					.member2(partner)
					.firstDate(LocalDate.now())
					.build()
			);
		}

		@AfterEach
		void tearDown() {
			datingRepository.deleteAllInBatch();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Test
		void 성공_member와coupleId와날짜를_입력하면_해당하는일정이조회되고_시작일오름차순으로정렬된다() {

			// Given
			Dating yesterday = Dating.builder()
				.title("yesterday")
				.couple(couple)
				.startDateTime(LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS))
				.endDateTime(LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS))
				.build();
			Dating today = Dating.builder()
				.title("today")
				.couple(couple)
				.startDateTime(LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS))
				.endDateTime(LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS))
				.build();
			Dating tomorrow = Dating.builder()
				.title("tomorrow")
				.couple(couple)
				.startDateTime(LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS))
				.endDateTime(LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS))
				.build();
			datingRepository.saveAll(List.of(
				yesterday,
				today,
				tomorrow
			));

			// Stubbing
			given(coupleReadService.findCoupleByMemberOrElseThrow(member))
				.willReturn(couple);

			// When
			LocalDate now = LocalDate.now();
			DatingServiceResponse response = datingReadService.readDating(
				couple.getId(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

			// Then
			DatingEntry datingEntryResponse = response.getDatingList().get(0);
			assertThat(response.getDatingList()).hasSize(1);
			assertThat(datingEntryResponse.getTitle()).isEqualTo(today.getTitle());
			assertThat(datingEntryResponse.getLocation()).isEqualTo(today.getLocation());
			assertThat(datingEntryResponse.getContent()).isEqualTo(today.getContent());
			assertThat(datingEntryResponse.getStartDateTime()).isEqualTo(today.getStartDateTime());
			assertThat(datingEntryResponse.getEndDateTime()).isEqualTo(today.getEndDateTime());
		}
	}

	@Nested
	@DisplayName("데이트 일정 id로 데이트 조회 시")
	class FindByDatingId {

		private Dating savedDating;

		@BeforeEach
		void setUp() {
			Member member = memberRepository.save(createMember("01012345678", "aaa"));
			Member partner = memberRepository.save(createMember("01012345679", "bbb"));
			Couple couple = coupleRepository.save(createCouple(member, partner));
			savedDating = datingRepository.save(createDating(LocalDate.now(), couple));
		}

		@AfterEach
		void tearDown() {
			datingRepository.deleteAllInBatch();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Test
		void 성공_존재하는id를입력하면_요청에해당하는데이트일정을반환한다() {

			Dating findDating = datingReadService.findByDatingId(savedDating.getId());

			assertThat(findDating.getId()).isEqualTo(savedDating.getId());
			assertThat(findDating.getTitle()).isEqualTo(savedDating.getTitle());
			assertThat(findDating.getLocation()).isEqualTo(savedDating.getLocation());
			assertThat(findDating.getContent()).isEqualTo(savedDating.getContent());
			assertThat(findDating.getStartDateTime()).isEqualTo(savedDating.getStartDateTime());
			assertThat(findDating.getEndDateTime()).isEqualTo(savedDating.getEndDateTime());
			assertThat(findDating.getCouple().getId()).isEqualTo(savedDating.getCouple().getId());
		}

		@Test
		void 실패_요청에해당하는_데이트일정이존재하지않는다면_예외를반환한다() {

			DatingNotFoundException exception = new DatingNotFoundException();
			assertThatThrownBy(() -> datingReadService.findByDatingId(savedDating.getId() + 100))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	private Dating createDating(LocalDate date, Couple couple) {
		return Dating.builder()
			.startDateTime(date.atTime(0, 0).truncatedTo(ChronoUnit.SECONDS))
			.endDateTime(date.atTime(23, 59).truncatedTo(ChronoUnit.SECONDS))
			.title("title")
			.couple(couple)
			.build();
	}

	private Couple createCouple(Member member, Member partner) {
		return Couple.builder()
			.member1(member)
			.member2(partner)
			.firstDate(LocalDate.now())
			.build();
	}

	private Member createMember(String phone, String nickname) {

		return Member.builder()
			.name("홍길동")
			.nickname(nickname)
			.phone(phone)
			.password("password")
			.gender(Gender.MALE)
			.birthDay(LocalDate.of(1999, 10, 10))
			.build();
	}
}
