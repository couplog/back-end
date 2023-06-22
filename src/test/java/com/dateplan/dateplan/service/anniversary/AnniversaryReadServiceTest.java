package com.dateplan.dateplan.service.anniversary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.dateplan.dateplan.domain.anniversary.dto.AnniversaryDatesServiceResponse;
import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryPatternRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryRepository;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryReadService;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
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

		private Member member;
		private Couple couple;
		private List<LocalDate> savedAnniversaryDates;

		@BeforeEach
		void setUp(TestInfo testInfo) {

			member = createMember("01011112222", "nickname1", LocalDate.of(1999, 10, 10));
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
			Integer month = 1;

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// when
			AnniversaryDatesServiceResponse serviceResponse = anniversaryReadService.readAnniversaryDates(
				member, coupleId, year, month);

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

		@DisplayName("대상 커플의 id 와 현재 로그인 회원의 커플 id 가 다르다면, 예외를 발생시킨다.")
		@Test
		void withDifferentValueLoginMemberCoupleIdAndTargetCoupleId() {

			// given
			Long targetCoupleId = couple.getId() + 100;
			Integer year = 2023;
			Integer month = 1;

			NoPermissionException expectedException = new NoPermissionException(Resource.COUPLE,
				Operation.READ);

			// stub
			given(coupleReadService.findCoupleByMemberOrElseThrow(any(Member.class)))
				.willReturn(couple);

			// when & then
			assertThatThrownBy(() -> anniversaryReadService.readAnniversaryDates(
				member, targetCoupleId, year, month))
				.isInstanceOf(expectedException.getClass())
				.hasMessage(expectedException.getMessage());
		}
	}

	private Anniversary createAnniversary(String title, LocalDate date,
		AnniversaryPattern anniversaryPattern) {

		return Anniversary.ofOther(title, null, date, anniversaryPattern);
	}

	private AnniversaryPattern createAnniversaryPattern(Couple couple, LocalDate date) {

		return AnniversaryPattern.builder()
			.couple(couple)
			.repeatStartDate(date)
			.repeatEndDate(date)
			.repeatRule(AnniversaryRepeatRule.NONE)
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
