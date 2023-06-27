package com.dateplan.dateplan.service.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceRequest;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleRepository;
import com.dateplan.dateplan.domain.schedule.service.ScheduleService;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.DateConstants;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.schedule.ScheduleNotFoundException;
import com.dateplan.dateplan.global.util.ScheduleDateUtil;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
			member = createMember("nickname");
			MemberThreadLocal.set(member);
			memberRepository.save(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
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
			scheduleService.createSchedule(memberId, request);

			// Then
			SchedulePattern schedulePattern = schedulePatternRepository.findAll().get(0);
			List<Schedule> schedules = scheduleRepository.findAll();

			// SchedulePattern assert
			assertThat(schedulePattern.getRepeatStartDate()).isEqualTo(
				request.getStartDateTime().toLocalDate());
			assertThat(schedulePattern.getRepeatEndDate()).isEqualTo(request.getRepeatEndTime());
			assertThat(schedulePattern.getRepeatRule()).isEqualTo(request.getRepeatRule());
			assertThat(schedulePattern.getMember().getId()).isEqualTo(memberId);

			// Schedule assert
			for (int i = 0; i < schedules.size(); i++) {
				Schedule schedule = schedules.get(i);
				assertThat(schedule.getContent()).isEqualTo(request.getContent());
				assertThat(schedule.getLocation()).isEqualTo(request.getLocation());
				assertThat(schedule.getTitle()).isEqualTo(request.getTitle());

				LocalDateTime nextStartDateTime = ScheduleDateUtil.getNextCycle(
					request.getStartDateTime(), request.getRepeatRule(), i);
				LocalDateTime nextEndDateTime = ScheduleDateUtil.getNextCycle(
					request.getEndDateTime(), request.getRepeatRule(), i);
				assertThat(schedule.getStartDateTime()).isEqualToIgnoringSeconds(nextStartDateTime);
				assertThat(schedule.getEndDateTime()).isEqualToIgnoringSeconds(nextEndDateTime);

				long diff = ChronoUnit.SECONDS.between(
					schedule.getStartDateTime(), schedule.getEndDateTime());
				assertThat(diff).isEqualTo(ChronoUnit.SECONDS.between(
					request.getStartDateTime(), request.getEndDateTime()));
			}
		}

		@DisplayName("로그인한 회원 외의 회원에 대한 요청을 하면 실패한다")
		@ParameterizedTest
		@EnumSource(value = RepeatRule.class, names = {"N", "D", "W", "M", "Y"})
		void FailWithNoPermissionRequest(RepeatRule repeatRule) {

			// Given
			Long memberId = member.getId();
			Long otherMemberId = memberId + 1;

			// When
			ScheduleServiceRequest request = createScheduleServiceRequest(repeatRule);

			// Then
			assertThatThrownBy(() -> scheduleService.createSchedule(otherMemberId, request))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(String.format(DetailMessage.NO_PERMISSION, Resource.MEMBER.getName(),
					Operation.CREATE.getName()));
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
			MemberThreadLocal.set(member);

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
			MemberThreadLocal.remove();
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

		@Tag(NEED_SCHEDULE)
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

		@DisplayName("일정이 모두 삭제되면 해당되는 일정 패턴도 삭제된다")
		@Test
		void removeCascadePattern() {

			// Given
			SchedulePattern schedulePattern = schedulePatternRepository.save(
				SchedulePattern.builder()
					.member(member)
					.repeatRule(RepeatRule.D)
					.repeatStartDate(LocalDate.now())
					.repeatEndDate(LocalDate.now().plusDays(2))
					.build()
			);
			List<Schedule> schedules = List.of(
				Schedule.builder()
					.title("title")
					.startDateTime(LocalDateTime.now())
					.endDateTime(LocalDateTime.now().plusDays(1))
					.schedulePattern(schedulePattern)
					.build(),
				Schedule.builder()
					.title("title")
					.startDateTime(LocalDateTime.now().plusDays(1))
					.endDateTime(LocalDateTime.now().plusDays(2))
					.schedulePattern(schedulePattern)
					.build()
			);
			scheduleRepository.saveAll(schedules);

			// When
			schedules.forEach(
				iterSchedule -> scheduleService.deleteSchedule(member.getId(), iterSchedule.getId(),
					member, false)
			);

			// Then
			assertThat(schedulePatternRepository.findById(schedulePattern.getId())).isEqualTo(
				Optional.empty());
			schedules.forEach(
				iterSchedule -> assertThat(
					scheduleRepository.findById(iterSchedule.getId())).isEqualTo(Optional.empty())
			);
		}
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
			.startDateTime(LocalDateTime.now().with(LocalTime.MIN))
			.endDateTime(LocalDateTime.now().with(LocalTime.MAX))
			.repeatRule(repeatRule)
			.build();
	}

	private Schedule createSchedule(SchedulePattern schedulePattern) {
		return Schedule.builder()
			.schedulePattern(schedulePattern)
			.title("title")
			.content("content")
			.location("location")
			.startDateTime(LocalDateTime.now())
			.endDateTime(LocalDateTime.now().plusDays(5))
			.build();
	}
}
