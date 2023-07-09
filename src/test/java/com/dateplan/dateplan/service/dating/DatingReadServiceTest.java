package com.dateplan.dateplan.service.dating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.domain.dating.repository.DatingQueryRepository;
import com.dateplan.dateplan.domain.dating.repository.DatingRepository;
import com.dateplan.dateplan.domain.dating.service.DatingReadService;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingDatesServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.global.exception.dating.DatingNotFoundException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
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

	@SpyBean
	private DatingQueryRepository queryRepository;

	@DisplayName("일정 날짜를 조회할 때")
	@Nested
	class ReadDatingDate {

		private static final String NEED_DATING = "needDating";

		private Member member;
		private Couple couple;
		private List<LocalDate> savedDatingDates;

		@BeforeEach
		void setUp(TestInfo testInfo) {
			member = memberRepository.save(createMember("01012345678", "aaa"));
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
				member, couple.getId(), null, null);

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
				member, couple.getId(), year, month);

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

		@DisplayName("로그인한 회원이 연결된 커플의 id와 요청한 coupldId가 다르면 실패한다.")
		@Test
		void failWithNoPermission() {

			// Stubbing
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.READ);
			assertThatThrownBy(
				() -> datingReadService.readDatingDates(member, couple.getId() + 100, null, null))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());

			// Verify
			then(queryRepository)
				.shouldHaveNoInteractions();
		}

		@DisplayName("회원이 연결되어 있지 않으면 실패한다.")
		@Test
		void failWithNotConnected() {

			// Stubbing
			MemberNotConnectedException exception = new MemberNotConnectedException();
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willThrow(exception);

			// When & Then
			assertThatThrownBy(
				() -> datingReadService.readDatingDates(member, couple.getId(), null, null))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());

			// Verify
			then(queryRepository)
				.shouldHaveNoInteractions();
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
