package com.dateplan.dateplan.service.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleRepository;
import com.dateplan.dateplan.domain.schedule.service.ScheduleService;
import com.dateplan.dateplan.domain.schedule.service.dto.request.ScheduleServiceRequest;
import com.dateplan.dateplan.domain.schedule.service.dto.request.ScheduleUpdateServiceRequest;
import com.dateplan.dateplan.global.constant.DateConstants;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.schedule.ScheduleNotFoundException;
import com.dateplan.dateplan.global.util.ScheduleDateUtil;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

public class ScheduleServiceTest extends ServiceTestSupport {

	@Autowired
	private ScheduleService scheduleService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SchedulePatternRepository schedulePatternRepository;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Nested
	@DisplayName("개인 일정을 생성할 때")
	class CreateSchedule {

		Member member;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("nickname"));
		}

		@AfterEach
		void tearDown() {
			scheduleRepository.deleteAllInBatch();
			schedulePatternRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("올바른 요청을 입력하면 성공한다.")
		@ParameterizedTest
		@EnumSource(value = RepeatRule.class, names = {"N", "D", "W", "M", "Y"})
		void successWithValidRequest(RepeatRule repeatRule) {

			// Given
			Long memberId = member.getId();
			ScheduleServiceRequest request = createScheduleServiceRequest(repeatRule);

			// When
			scheduleService.createSchedule(member, memberId, request);

			// Then
			SchedulePattern schedulePattern = schedulePatternRepository.findAll().get(0);
			List<Schedule> schedules = scheduleRepository.findAll();

			// SchedulePattern assert
			assertThat(schedulePattern.getRepeatStartDate()).isEqualTo(
				request.getStartDateTime().toLocalDate());
			assertThat(schedulePattern.getRepeatEndDate()).isEqualTo(request.getRepeatEndTime());
			assertThat(schedulePattern.getRepeatRule()).isEqualTo(request.getRepeatRule());
			assertThat(schedulePattern.getMember().getId()).isEqualTo(memberId);

			int cycleCount = 0;
			// Schedule assert
			for (int i = 0; i < schedules.size(); i++, cycleCount++) {
				Schedule schedule = schedules.get(i);
				assertThat(schedule.getContent()).isEqualTo(request.getContent());
				assertThat(schedule.getLocation()).isEqualTo(request.getLocation());
				assertThat(schedule.getTitle()).isEqualTo(request.getTitle());

				LocalDateTime nextStartDateTime = ScheduleDateUtil.getNextCycle(
					request.getStartDateTime(), request.getRepeatRule(), cycleCount);
				LocalDateTime nextEndDateTime = ScheduleDateUtil.getNextCycle(
					request.getEndDateTime(), request.getRepeatRule(), cycleCount);

				long diff = ChronoUnit.SECONDS.between(
					schedule.getStartDateTime(), schedule.getEndDateTime());
				assertThat(diff).isEqualTo(ChronoUnit.SECONDS.between(
					request.getStartDateTime(), request.getEndDateTime()));

				if (Objects.equals(request.getRepeatRule(), RepeatRule.D) ||
					Objects.equals(request.getRepeatRule(), RepeatRule.W) ||
					nextStartDateTime.getDayOfMonth() == request.getStartDateTime().getDayOfMonth()
				) {
					assertThat(schedule.getStartDateTime()).isEqualToIgnoringSeconds(
						nextStartDateTime);
					assertThat(schedule.getEndDateTime()).isEqualToIgnoringSeconds(nextEndDateTime);
				} else {
					cycleCount++;
				}
			}
		}

		@DisplayName("현재 로그인한 회원의 id와 요청의 member_id가 다르면 실패한다.")
		@ParameterizedTest
		@EnumSource(value = RepeatRule.class, names = {"N", "D", "W", "M", "Y"})
		void FailWithNoPermissionRequest(RepeatRule repeatRule) {

			// Given
			Long memberId = member.getId();
			Long otherMemberId = memberId + 1;

			// When
			ScheduleServiceRequest request = createScheduleServiceRequest(repeatRule);

			// Then
			NoPermissionException exception = new NoPermissionException(Resource.MEMBER,
				Operation.CREATE);
			assertThatThrownBy(() -> scheduleService.createSchedule(member, otherMemberId, request))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	@DisplayName("일정 수정 시")
	@Nested
	class UpdateSchedule {

		private static final String NEED_SCHEDULE = "needSchedule";

		Member member;
		List<Schedule> schedules;

		@BeforeEach
		void setUp(TestInfo testInfo) {
			member = memberRepository.save(createMember("nickname"));

			if (testInfo.getTags().contains(NEED_SCHEDULE)) {
				SchedulePattern schedulePattern = schedulePatternRepository.save(
					SchedulePattern.builder()
						.member(member)
						.repeatRule(RepeatRule.N)
						.repeatStartDate(LocalDate.now())
						.repeatEndDate(DateConstants.CALENDER_END_DATE)
						.build()
				);
				schedules = List.of(
					createSchedule(schedulePattern),
					createSchedule(schedulePattern),
					createSchedule(schedulePattern),
					createSchedule(schedulePattern),
					createSchedule(schedulePattern)
				);
				scheduleRepository.saveAll(schedules);
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
		@DisplayName("단일 일정 수정하는 요청 시, 요청의 내용으로 해당 일정이 수정된다.")
		@Test
		void successWithSingleUpdateRequest() {
			// Given
			ScheduleUpdateServiceRequest request = createScheduleUpdateServiceRequest();
			Schedule schedule = schedules.get(0);

			// When
			scheduleService.updateSchedule(member.getId(), schedule.getId(), request, member,
				false);

			// Then
			Schedule updatedSchedule = scheduleRepository.findById(schedule.getId()).get();

			assertThat(updatedSchedule.getTitle()).isEqualTo(request.getTitle());
			assertThat(updatedSchedule.getContent()).isEqualTo(request.getContent());
			assertThat(updatedSchedule.getLocation()).isEqualTo(request.getLocation());
			assertThat(updatedSchedule.getStartDateTime()).isEqualTo(request.getStartDateTime());
			assertThat(updatedSchedule.getEndDateTime()).isEqualTo(request.getEndDateTime());
		}

		@Tag(NEED_SCHEDULE)
		@DisplayName("반복 일정 수정하는 요청 시, 요청의 내용으로 모든 일정이 수정된다.")
		@Test
		void successWithRepeatUpdateRequest() {
			// Given
			ScheduleUpdateServiceRequest request = createScheduleUpdateServiceRequest();
			Schedule schedule = schedules.get(0);

			// When
			scheduleService.updateSchedule(member.getId(), schedule.getId(), request, member,
				true);

			// Then
			List<Schedule> updatedSchedules = scheduleRepository.findBySchedulePatternId(
				schedule.getSchedulePattern().getId());

			long startTimeDiff = ChronoUnit.MINUTES.between(schedule.getStartDateTime(),
				request.getStartDateTime());
			long endTimeDiff = ChronoUnit.MINUTES.between(schedule.getEndDateTime(),
				request.getEndDateTime());

			for (int i = 0; i < updatedSchedules.size(); i++) {
				assertThat(updatedSchedules.get(i).getTitle()).isEqualTo(request.getTitle());
				assertThat(updatedSchedules.get(i).getContent()).isEqualTo(request.getContent());
				assertThat(updatedSchedules.get(i).getLocation()).isEqualTo(request.getLocation());
				assertThat(updatedSchedules.get(i).getStartDateTime()).isEqualTo(
					schedules.get(i).getStartDateTime().plus(startTimeDiff, ChronoUnit.MINUTES));
				assertThat(updatedSchedules.get(i).getEndDateTime()).isEqualTo(
					schedules.get(i).getEndDateTime().plus(endTimeDiff, ChronoUnit.MINUTES));
			}
		}

		@DisplayName("현재 로그인한 회원의 id와 요청의 member_id가 다르면 실패한다.")
		@Test
		void failWithNoPermission() {

			// Given
			ScheduleUpdateServiceRequest request = createScheduleUpdateServiceRequest();

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.MEMBER,
				Operation.UPDATE);
			assertThatThrownBy(() ->
				scheduleService.updateSchedule(member.getId() + 100, 1L, request, member, true))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());

		}

		@DisplayName("요청에 해당하는 일정을 찾을 수 없으면 실패한다")
		@Test
		void failWIthScheduleNotFound() {
			// Given
			ScheduleUpdateServiceRequest request = createScheduleUpdateServiceRequest();

			// When & Then
			ScheduleNotFoundException exception = new ScheduleNotFoundException();
			assertThatThrownBy(() ->
				scheduleService.updateSchedule(
					member.getId(), 100000L, request, member, true))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());

		}

		@Tag(NEED_SCHEDULE)
		@Test
		void 실패_요청한memberId와_조회한개인일정의coupleId가다르면_예외를반환한다() {
			Member otherMember = memberRepository.save(Member.builder()
				.phone("01011112222")
				.name("name")
				.nickname("ccc")
				.gender(Gender.MALE)
				.birthDay(LocalDate.now())
				.password("password")
				.build());

			SchedulePattern schedulePattern = schedulePatternRepository.save(
				SchedulePattern.builder()
					.member(otherMember)
					.repeatStartDate(LocalDate.now())
					.repeatEndDate(LocalDate.now())
					.repeatRule(RepeatRule.N)
					.build());
			Schedule otherSchedule = scheduleRepository.save(Schedule.builder()
				.title("otherDating")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now())
				.schedulePattern(schedulePattern)
				.build());

			ScheduleUpdateServiceRequest request = createScheduleUpdateServiceRequest();

			NoPermissionException exception = new NoPermissionException(Resource.SCHEDULE,
				Operation.UPDATE);

			assertThatThrownBy(() ->
				scheduleService.updateSchedule(member.getId(), otherSchedule.getId(), request,
					member, false))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	@Nested
	@DisplayName("일정 삭제 시")
	class DeleteSchedule {

		private static final String NEED_SCHEDULE = "needSchedule";

		Member member;
		List<Schedule> schedules;

		@BeforeEach
		void setUp(TestInfo testInfo) {
			member = memberRepository.save(createMember("nickname"));

			if (testInfo.getTags().contains(NEED_SCHEDULE)) {
				SchedulePattern schedulePattern = schedulePatternRepository.save(
					SchedulePattern.builder()
						.member(member)
						.repeatRule(RepeatRule.N)
						.repeatStartDate(LocalDate.now())
						.repeatEndDate(DateConstants.CALENDER_END_DATE)
						.build()
				);
				schedules = List.of(
					createSchedule(schedulePattern),
					createSchedule(schedulePattern),
					createSchedule(schedulePattern),
					createSchedule(schedulePattern),
					createSchedule(schedulePattern)
				);
				scheduleRepository.saveAll(schedules);
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
		@DisplayName("올바른 memberId, scheduleId, deleteRepeat = false를 요청하면 일정이 단일 삭제된다.")
		@Test
		void successWithDeleteSingleSchedule() {
			// Given
			Schedule schedule = schedules.get(0);

			// When
			scheduleService.deleteSchedule(member.getId(), schedule.getId(), member, false);

			// Then
			assertThat(scheduleRepository.findById(schedule.getId())).isEqualTo(Optional.empty());
			assertThat(schedulePatternRepository.findById(
				schedule.getSchedulePattern().getId())).isPresent();
		}

		@Tag(NEED_SCHEDULE)
		@DisplayName("올바른 memberId, scheduleId, deleteRepeat = true를 요청하면 반복 일정이 모두 삭제된다.")
		@Test
		void successWithDeleteRepeatSchedule() {

			// Given
			Schedule schedule = schedules.get(0);

			// When
			scheduleService.deleteSchedule(member.getId(), schedule.getId(), member, true);

			// Then
			assertThat(scheduleRepository.findById(schedule.getId())).isEqualTo(Optional.empty());
			assertThat(schedulePatternRepository.findById(
				schedule.getSchedulePattern().getId())).isEmpty();
		}

		@DisplayName("요청한 회원의 id와 로그인한 회원의 id가 다르면 실패한다.")
		@Test
		void failWithNoPermission() {

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.MEMBER,
				Operation.DELETE);
			assertThatThrownBy(
				() -> scheduleService.deleteSchedule(member.getId() + 10, 1L, member, false))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}

		@DisplayName("요청한 일정이 존재하지 않으면 실패한다")
		@Test
		void failWithScheduleNotFound() {

			// When & Then
			ScheduleNotFoundException exception = new ScheduleNotFoundException();
			assertThatThrownBy(
				() -> scheduleService.deleteSchedule(member.getId(), 1000000L, member, false))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}

		@Tag(NEED_SCHEDULE)
		@DisplayName("일정이 모두 삭제되면 해당되는 일정 패턴도 삭제된다")
		@Test
		void removeCascadePattern() {

			// When
			schedules.forEach(
				iterSchedule -> scheduleService.deleteSchedule(member.getId(), iterSchedule.getId(),
					member, false)
			);

			// Then
			schedules.forEach(
				iterSchedule -> {
					assertThat(schedulePatternRepository.findById(
						iterSchedule.getSchedulePattern().getId())).isEmpty();
					assertThat(scheduleRepository.findById(iterSchedule.getId())).isEmpty();
				}
			);
		}

		@Tag(NEED_SCHEDULE)
		@Test
		void 실패_요청한memberId와_조회한개인일정의coupleId가다르면_예외를반환한다() {
			Member otherMember = memberRepository.save(Member.builder()
				.phone("01011112222")
				.name("name")
				.nickname("ccc")
				.gender(Gender.MALE)
				.birthDay(LocalDate.now())
				.password("password")
				.build());

			SchedulePattern schedulePattern = schedulePatternRepository.save(
				SchedulePattern.builder()
					.member(otherMember)
					.repeatStartDate(LocalDate.now())
					.repeatEndDate(LocalDate.now())
					.repeatRule(RepeatRule.N)
					.build());
			Schedule otherSchedule = scheduleRepository.save(Schedule.builder()
				.title("otherDating")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now())
				.schedulePattern(schedulePattern)
				.build());

			ScheduleUpdateServiceRequest request = createScheduleUpdateServiceRequest();

			NoPermissionException exception = new NoPermissionException(Resource.SCHEDULE,
				Operation.DELETE);

			assertThatThrownBy(() ->
				scheduleService.deleteSchedule(member.getId(), otherSchedule.getId(), member,
					false))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	private Schedule createSchedule(SchedulePattern schedulePattern) {
		return Schedule.builder()
			.schedulePattern(schedulePattern)
			.title("title")
			.content("content")
			.location("location")
			.startDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
			.endDateTime(LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.SECONDS))
			.build();
	}

	private ScheduleUpdateServiceRequest createScheduleUpdateServiceRequest() {
		return ScheduleUpdateServiceRequest.builder()
			.title("new Title")
			.content("new Content")
			.location("new Location")
			.startDateTime(LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.SECONDS))
			.endDateTime(LocalDateTime.now().plusDays(20).truncatedTo(ChronoUnit.SECONDS))
			.build();
	}


	private Member createMember(String nickname) {

		return Member.builder()
			.name("홍길동")
			.nickname(nickname)
			.phone("01012341234")
			.password("password")
			.gender(Gender.MALE)
			.birthDay(LocalDate.of(1999, 10, 10))
			.build();
	}

	private ScheduleServiceRequest createScheduleServiceRequest(RepeatRule repeatRule) {
		return ScheduleServiceRequest.builder()
			.title("title")
			.startDateTime(LocalDateTime.now().with(LocalTime.MIN).truncatedTo(ChronoUnit.MINUTES))
			.endDateTime(LocalDateTime.now().with(LocalTime.MAX).truncatedTo(ChronoUnit.MINUTES))
			.repeatRule(repeatRule)
			.build();
	}
}
