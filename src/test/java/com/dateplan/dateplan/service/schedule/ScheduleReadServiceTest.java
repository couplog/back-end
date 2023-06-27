package com.dateplan.dateplan.service.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleDatesServiceResponse;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleQueryRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleRepository;
import com.dateplan.dateplan.domain.schedule.service.ScheduleReadService;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.global.exception.schedule.ScheduleNotFoundException;
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
import org.springframework.boot.test.mock.mockito.SpyBean;

public class ScheduleReadServiceTest extends ServiceTestSupport {

	@Autowired
	private CoupleRepository coupleRepository;

	@MockBean
	private CoupleReadService coupleReadService;

	@SpyBean
	private ScheduleQueryRepository queryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ScheduleReadService scheduleReadService;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private SchedulePatternRepository schedulePatternRepository;

	@DisplayName("일정 날짜를 조회할 때")
	@Nested
	class ReadSchedulesDate {

		Member member;
		Member partner;
		Couple couple;
		List<LocalDate> savedScheduleDates;
		List<Schedule> savedSchedules;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("01012345678", "aaa"));
			partner = memberRepository.save(createMember("01012345679", "bbb"));
			couple = coupleRepository.save(createCouple(member, partner));
			MemberThreadLocal.set(member);

			LocalDate now = LocalDate.now();

			SchedulePattern schedulePattern = schedulePatternRepository.save(createSchedulePattern(
				now, now.withDayOfMonth(now.lengthOfMonth()), member));

			savedScheduleDates = now.datesUntil(now.withDayOfMonth(now.lengthOfMonth()))
				.toList();
			savedSchedules = savedScheduleDates.stream()
				.map((date -> createSchedule(date, schedulePattern)))
				.toList();
			scheduleRepository.saveAll(savedSchedules);
		}

		@AfterEach
		void tearDown() {
			coupleRepository.deleteAllInBatch();
			scheduleRepository.deleteAllInBatch();
			schedulePatternRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("올바른 memberId, year, month를 요청하면 성공한다")
		@Test
		void successWithValidRequest() {

			given(coupleReadService.getPartnerId(any(Member.class)))
				.willReturn(partner.getId());

			ScheduleDatesServiceResponse response = scheduleReadService.readSchedule(
				member.getId(), null, null);

			List<LocalDate> actualDates = response.getScheduleDates();
			assertThat(actualDates)
				.containsExactlyElementsOf(savedScheduleDates)
				.isSortedAccordingTo(LocalDate::compareTo);
		}

		@DisplayName("로그인한 회원과 요청한 memberId가 다르면 실패한다.")
		@Test
		void failWithNoPermission() {

			assertThatThrownBy(
				() -> scheduleReadService.readSchedule(member.getId() + 100, null, null))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(String.format(DetailMessage.NO_PERMISSION, Resource.MEMBER.getName(),
					Operation.READ.getName()));

			then(queryRepository)
				.shouldHaveNoInteractions();
		}

		@DisplayName("회원이 연결되어 있지 않으면 실패한다.")
		@Test
		void failWithNotConnected() {

			given(coupleReadService.getPartnerId(any(Member.class)))
				.willThrow(new MemberNotConnectedException());

			assertThatThrownBy(() -> scheduleReadService.readSchedule(member.getId(), null, null))
				.isInstanceOf(MemberNotConnectedException.class)
				.hasMessage(DetailMessage.Member_NOT_CONNECTED);

			then(queryRepository)
				.shouldHaveNoInteractions();
		}
	}

	@DisplayName("일정 조회 시")
	@Nested
	class ReadSchedule {

		private static final String NEED_SCHEDULE = "needSchedule";

		Member member;
		Schedule schedule;

		@BeforeEach
		void setUp(TestInfo testInfo) {
			member = memberRepository.save(createMember("01012345678", "nickname"));

			if (testInfo.getTags().contains(NEED_SCHEDULE)) {
				SchedulePattern schedulePattern = schedulePatternRepository
					.save(createSchedulePattern(LocalDate.now(), LocalDate.now(), member));
				schedule = scheduleRepository.save(
					createSchedule(LocalDate.now(), schedulePattern));
			}
		}

		@AfterEach
		void tearDown(TestInfo testInfo) {
			if (testInfo.getTags().contains(NEED_SCHEDULE)) {
				scheduleRepository.deleteAllInBatch();
				schedulePatternRepository.deleteAllInBatch();
			}
			memberRepository.deleteAllInBatch();
		}

		@Tag(NEED_SCHEDULE)
		@DisplayName("올바른 일정 id를 입력하면, 해당 id에 해당하는 일정을 반환한다")
		@Test
		void successWithValidRequest() {

			Schedule findSchedule = scheduleReadService
				.findScheduleByIdOrElseThrow(schedule.getId());

			assertThat(findSchedule.getId()).isEqualTo(schedule.getId());
			assertThat(findSchedule.getTitle()).isEqualTo(schedule.getTitle());
			assertThat(findSchedule.getStartDateTime()).isEqualTo(schedule.getStartDateTime());
			assertThat(findSchedule.getEndDateTime()).isEqualTo(schedule.getEndDateTime());
			assertThat(findSchedule.getSchedulePattern().getId()).isEqualTo(
				schedule.getSchedulePattern().getId());
		}

		@DisplayName("존재하지 않는 일정 id를 요청하면 실패한다")
		@Test
		void failWithInvalidRequest() {

			ScheduleNotFoundException exception = new ScheduleNotFoundException();

			assertThatThrownBy(() -> scheduleReadService.findScheduleByIdOrElseThrow(100000L))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	private Schedule createSchedule(LocalDate date, SchedulePattern schedulePattern) {
		return Schedule.builder()
			.schedulePattern(schedulePattern)
			.startDateTime(date.atTime(0, 0))
			.endDateTime(date.atTime(23, 59))
			.title("title")
			.build();
	}

	private SchedulePattern createSchedulePattern(LocalDate startDate, LocalDate endDate,
		Member member) {
		return SchedulePattern.builder()
			.repeatRule(RepeatRule.D)
			.repeatStartDate(startDate)
			.repeatEndDate(endDate)
			.member(member)
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
