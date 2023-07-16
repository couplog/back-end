package com.dateplan.dateplan.service.calender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dateplan.dateplan.domain.anniversary.entity.Anniversary;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryCategory;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryPattern;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryPatternRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryRepository;
import com.dateplan.dateplan.domain.calender.service.CalenderReadService;
import com.dateplan.dateplan.domain.calender.service.dto.response.CalenderDateServiceResponse;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.domain.dating.repository.DatingRepository;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.entity.SchedulePattern;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleRepository;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CalenderReadServiceTest extends ServiceTestSupport {

	@Autowired
	private CalenderReadService calenderReadService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CoupleRepository coupleRepository;

	@Autowired
	private DatingRepository datingRepository;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private SchedulePatternRepository schedulePatternRepository;

	@Autowired
	private AnniversaryRepository anniversaryRepository;

	@Autowired
	private AnniversaryPatternRepository anniversaryPatternRepository;

	@Nested
	@DisplayName("일정 날짜 전체 조회 시")
	class ReadCalenderDate {

		private Member member;
		private Member partner;
		private Couple couple;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("01011112222", "aaa"));
			partner = memberRepository.save(createMember("01011113333", "bbb"));
			couple = coupleRepository.save(createCouple(member, partner));
		}

		@AfterEach
		void tearDown() {
			datingRepository.deleteAllInBatch();
			anniversaryRepository.deleteAllInBatch();
			anniversaryPatternRepository.deleteAllInBatch();
			scheduleRepository.deleteAllInBatch();
			schedulePatternRepository.deleteAllInBatch();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("[성공] 올바른 memberId, year, month를 입력하면 "
			+ "yearMonth의 각 날짜에 존재하는 일정들이 dating, my, partner, anniversary 순으로 조회된다")
		@Test
		void should_returnExistScheduleByDatesOrderBy_When_validRequest() {

			datingRepository.save(
				Dating.builder()
					.title("dating")
					.startDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
					.endDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
					.couple(couple)
					.build()
			);

			SchedulePattern mySchedulePattern = schedulePatternRepository.save(
				SchedulePattern.builder()
					.member(member)
					.repeatStartDate(LocalDate.now())
					.repeatEndDate(LocalDate.now())
					.repeatRule(RepeatRule.D)
					.build()
			);
			scheduleRepository.save(
				Schedule.builder()
					.title("my")
					.startDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
					.endDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
					.schedulePattern(mySchedulePattern)
					.build()
			);

			SchedulePattern partnerSchedulePattern = schedulePatternRepository.save(
				SchedulePattern.builder()
					.member(partner)
					.repeatStartDate(LocalDate.now())
					.repeatEndDate(LocalDate.now())
					.repeatRule(RepeatRule.D)
					.build()
			);
			scheduleRepository.save(
				Schedule.builder()
					.title("partner")
					.startDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
					.endDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
					.schedulePattern(partnerSchedulePattern)
					.build()
			);

			AnniversaryPattern anniversaryPattern = anniversaryPatternRepository.save(
				AnniversaryPattern.builder()
					.couple(couple)
					.repeatStartDate(LocalDate.now())
					.repeatEndDate(LocalDate.now())
					.repeatRule(AnniversaryRepeatRule.NONE)
					.category(AnniversaryCategory.OTHER)
					.build()
			);
			anniversaryRepository.save(
				Anniversary.builder()
					.title("기념일")
					.anniversaryPattern(anniversaryPattern)
					.date(LocalDate.now())
					.build()
			);

			CalenderDateServiceResponse response = calenderReadService.readCalenderDates(
				member, member.getId(), LocalDate.now()
					.getYear(), LocalDate.now().getMonthValue());

			assertThat(response.getSchedules().get(0).getDate()).isEqualTo(LocalDate.now());
			assertThat(response.getSchedules().get(0).getEvents()).hasSize(4);
			assertThat(response.getSchedules().get(0).getEvents().get(0)).isEqualTo(
				"datingSchedule");
			assertThat(response.getSchedules().get(0).getEvents().get(1)).isEqualTo("mySchedule");
			assertThat(response.getSchedules().get(0).getEvents().get(2)).isEqualTo(
				"partnerSchedule");
			assertThat(response.getSchedules().get(0).getEvents().get(3)).isEqualTo("anniversary");
		}

		@DisplayName("[실패] 요청한 memberId와 현재 회원의 id가 다르면 실패한다")
		@Test
		void should_throwNoPermissionException_When_mismatchMemberId() {

			NoPermissionException exception = new NoPermissionException(Resource.MEMBER,
				Operation.READ);
			assertThatThrownBy(
				() -> calenderReadService.readCalenderDates(member, member.getId() + 1,
					LocalDate.now()
						.getYear(), LocalDate.now().getMonthValue()))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
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
