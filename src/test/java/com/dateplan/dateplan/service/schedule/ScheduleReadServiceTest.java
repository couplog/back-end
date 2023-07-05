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
import com.dateplan.dateplan.domain.schedule.controller.dto.response.ScheduleEntry;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleQueryRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleRepository;
import com.dateplan.dateplan.domain.schedule.service.ScheduleReadService;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleDatesServiceResponse;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleServiceResponse;
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
import java.util.ArrayList;
import java.util.Comparator;
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
			MemberThreadLocal.remove();
		}

		@DisplayName("올바른 memberId, year, month를 요청하면 성공한다")
		@Test
		void successWithValidRequest() {

			given(coupleReadService.getPartnerId(any(Member.class)))
				.willReturn(partner.getId());

			ScheduleDatesServiceResponse response = scheduleReadService.readScheduleDates(
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
				() -> scheduleReadService.readScheduleDates(member.getId() + 100, null, null))
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

			assertThatThrownBy(
				() -> scheduleReadService.readScheduleDates(member.getId(), null, null))
				.isInstanceOf(MemberNotConnectedException.class)
				.hasMessage(DetailMessage.Member_NOT_CONNECTED);

			then(queryRepository)
				.shouldHaveNoInteractions();
		}
	}

	@DisplayName("일정 상세 조회 시")
	@Nested
	class ReadSchedules {

		private static final String NEED_SCHEDULES = "needSchedules";
		private Member member;
		private Member partner;
		private Couple couple;
		private final List<ScheduleEntry> schedules = new ArrayList<>();
		private final List<ScheduleEntry> partnerSchedules = new ArrayList<>();

		@BeforeEach
		void setUp(TestInfo testInfo) {
			member = createMember("01012345678", "aaa");
			partner = createMember("01012345679", "bbb");
			couple = createCouple(member, partner);
			memberRepository.saveAll(List.of(member, partner));
			coupleRepository.save(couple);

			if (testInfo.getTags().contains(NEED_SCHEDULES)) {
				SchedulePattern pattern = schedulePatternRepository.save(
					createSchedulePattern(LocalDate.now(), LocalDate.now(), member));
				for (int i = 0; i < 5; i++) {
					Schedule savedSchedule = createSchedule(LocalDate.now(), pattern);
					schedules.add(
						ScheduleEntry.from(scheduleRepository.save(savedSchedule)));
				}

				SchedulePattern partnerPattern = schedulePatternRepository.save(
					createSchedulePattern(LocalDate.now(), LocalDate.now(), partner));
				for (int i = 0; i < 5; i++) {
					Schedule savedSchedule = createSchedule(LocalDate.now(), partnerPattern);
					partnerSchedules.add(
						ScheduleEntry.from(scheduleRepository.save(savedSchedule)));
				}
			}
		}

		@AfterEach
		void tearDown(TestInfo testInfo) {
			if (testInfo.getTags().contains(NEED_SCHEDULES)) {
				scheduleRepository.deleteAllInBatch();
				schedulePatternRepository.deleteAllInBatch();
			}
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Tag(NEED_SCHEDULES)
		@DisplayName("올바른 id, member, 날짜가 요청되면 성공하고, 일정 일시 오름차순으로 정렬되어 반환한다")
		@Test
		void successWithValidRequest() {

			//Stubbing
			given(coupleReadService.getPartnerId(any(Member.class)))
				.willReturn(partner.getId());

			// Given
			ScheduleServiceResponse scheduleServiceResponse = scheduleReadService.readSchedules(
				member.getId(), member,
				LocalDate.now().getYear(),
				LocalDate.now().getMonthValue(),
				LocalDate.now().getDayOfMonth());

			// When & Then
			for (int i = 0; i < scheduleServiceResponse.getSchedules().size(); i++) {
				ScheduleEntry actual = scheduleServiceResponse.getSchedules().get(i);
				assertThat(actual.getScheduleId()).isEqualTo(schedules.get(i).getScheduleId());
				assertThat(actual.getTitle()).isEqualTo(schedules.get(i).getTitle());
				assertThat(actual.getContent()).isEqualTo(schedules.get(i).getContent());
				assertThat(actual.getLocation()).isEqualTo(schedules.get(i).getLocation());
				assertThat(actual.getStartDateTime()).isEqualTo(
					schedules.get(i).getStartDateTime());
				assertThat(actual.getEndDateTime()).isEqualTo(schedules.get(i).getEndDateTime());
				assertThat(actual.getRepeatRule()).isEqualTo(schedules.get(i).getRepeatRule());
			}
			assertThat(scheduleServiceResponse.getSchedules())
				.extracting(ScheduleEntry::getStartDateTime)
				.isSortedAccordingTo(Comparator.naturalOrder());
		}

		@DisplayName("상대방의 id로 요청하면, 상대방의 일정이 조회된다")
		@Test
		void successWithPartnerId() {

			//Stubbing
			given(coupleReadService.getPartnerId(any(Member.class)))
				.willReturn(partner.getId());

			// Given
			ScheduleServiceResponse scheduleServiceResponse = scheduleReadService.readSchedules(
				partner.getId(), member,
				LocalDate.now().getYear(),
				LocalDate.now().getMonthValue(),
				LocalDate.now().getDayOfMonth());

			// When & Then
			for (int i = 0; i < scheduleServiceResponse.getSchedules().size(); i++) {
				ScheduleEntry actual = scheduleServiceResponse.getSchedules().get(i);
				assertThat(actual.getScheduleId()).isEqualTo(
					partnerSchedules.get(i).getScheduleId());
				assertThat(actual.getTitle()).isEqualTo(partnerSchedules.get(i).getTitle());
				assertThat(actual.getContent()).isEqualTo(partnerSchedules.get(i).getContent());
				assertThat(actual.getLocation()).isEqualTo(partnerSchedules.get(i).getLocation());
				assertThat(actual.getStartDateTime()).isEqualTo(
					partnerSchedules.get(i).getStartDateTime());
				assertThat(actual.getEndDateTime()).isEqualTo(
					partnerSchedules.get(i).getEndDateTime());
				assertThat(actual.getRepeatRule()).isEqualTo(
					partnerSchedules.get(i).getRepeatRule());
			}

			assertThat(scheduleServiceResponse.getSchedules())
				.extracting(ScheduleEntry::getStartDateTime)
				.isSortedAccordingTo(Comparator.naturalOrder());
		}

		@DisplayName("요청한 member_id가 회원 또는 연결된 회원의 id가 아니면 실패한다")
		@Test
		void failWithNoPermission() {

			// Given
			LocalDate now = LocalDate.now();
			NoPermissionException exception =
				new NoPermissionException(Resource.MEMBER, Operation.READ);

			// When & Then
			assertThatThrownBy(
				() -> scheduleReadService.readSchedules(partner.getId() + 100,
					member, now.getYear(), now.getMonthValue(), now.getDayOfMonth()))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
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

	@DisplayName("일정 패턴 아이디로 일정 리스트 조회 시")
	@Nested
	class FindBySchedulePatternId {

		private static final String NEED_SCHEDULE = "needSchedule";

		Member member;
		List<Schedule> schedules;

		@BeforeEach
		void setUp(TestInfo testInfo) {
			member = memberRepository.save(createMember("01012345678", "nickname"));

			if (testInfo.getTags().contains(NEED_SCHEDULE)) {
				SchedulePattern schedulePattern = schedulePatternRepository
					.save(createSchedulePattern(LocalDate.now(), LocalDate.now(), member));
				schedules = scheduleRepository.saveAll(List.of(
					createSchedule(LocalDate.now(), schedulePattern),
					createSchedule(LocalDate.now(), schedulePattern),
					createSchedule(LocalDate.now(), schedulePattern),
					createSchedule(LocalDate.now(), schedulePattern)
				));
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
		@DisplayName("일정이 존재하는 일정 패턴 id를 입력하면 해당하는 모든 일정들이 조회된다.")
		@Test
		void successWithValidRequest() {

			// Given
			Long schedulePatternId = schedules.get(0).getSchedulePattern().getId();

			// When
			List<Schedule> savedSchedules = scheduleReadService.findBySchedulePatternId(
				schedulePatternId);

			// Then
			assertThat(savedSchedules).hasSize(schedules.size());
			for (int i = 0; i < schedules.size(); i++) {
				assertThat(schedules.get(i).getTitle()).isEqualTo(savedSchedules.get(i).getTitle());
				assertThat(schedules.get(i).getContent()).isEqualTo(
					savedSchedules.get(i).getContent());
				assertThat(schedules.get(i).getLocation()).isEqualTo(
					savedSchedules.get(i).getLocation());
				assertThat(schedules.get(i).getStartDateTime()).isEqualTo(
					savedSchedules.get(i).getStartDateTime());
				assertThat(schedules.get(i).getEndDateTime()).isEqualTo(
					savedSchedules.get(i).getEndDateTime());
				assertThat(schedules.get(i).getId()).isEqualTo(savedSchedules.get(i).getId());
				assertThat(schedules.get(i).getSchedulePattern().getId()).isEqualTo(
					savedSchedules.get(i).getSchedulePattern().getId());
			}
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
			.repeatRule(RepeatRule.N)
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
