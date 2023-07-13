package com.dateplan.dateplan.controller.calendar;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.calender.controller.dto.response.CalenderEntry;
import com.dateplan.dateplan.domain.calender.service.dto.response.CalenderDateServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CalenderControllerTest extends ControllerTestSupport {

	@BeforeEach
	void setUp() {
		given(
			authInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class),
				any(Object.class)))
			.willReturn(true);
	}

	@Nested
	@DisplayName("일정 날짜 전체 조회 시")
	class ReadCalenderDates {

		private static final String REQUEST_URL = "/api/members/{member_id}/calender/date";

		@BeforeEach
		void setUp() {
			Member member = createMember();
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("[성공] 올바른 member_id, coupleId, year, month를 입력하면 해당 연월에 존재하는 일정들이 반환된다.")
		@Test
		void should_returnSchedules_When_validRequest() throws Exception {

			// Given
			CalenderDateServiceResponse response = CalenderDateServiceResponse.builder()
				.schedules(List.of(
					CalenderEntry.builder()
						.date(LocalDate.of(2023, 7, 13))
						.events(List.of("anniversary", "mySchedule"))
						.build(),
					CalenderEntry.builder()
						.date(LocalDate.of(2023, 7, 15))
						.events(List.of("anniversary", "partnerSchedule"))
						.build(),
					CalenderEntry.builder()
						.date(LocalDate.of(2023, 7, 13))
						.events(List.of("anniversary", "datingSchedule"))
						.build()
				))
				.build();

			// Stubbing
			given(calenderReadService.readCalenderDates(any(Member.class), anyLong(), anyLong(),
				anyInt(), anyInt()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L)
					.param("coupleId", "1")
					.param("year", String.valueOf(7))
					.param("month", String.valueOf(13)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(jsonPath("$.data.schedules[0].date").value(
					LocalDate.of(2023, 7, 13).toString()))
				.andExpect(
					jsonPath("$.data.schedules[0].events[0]").value(containsString("anniversary")))
				.andExpect(
					jsonPath("$.data.schedules[0].events[1]").value(containsString("mySchedule")));
		}

		@DisplayName("[실패] 현재 로그인한 회원의 id와 요청의 member_id가 다르면 실패한다.")
		@Test
		void should_returnNoPermissionException_When_mismatchMemberId() throws Exception {

			// Stubbing
			NoPermissionException exception = new NoPermissionException(Resource.MEMBER,
				Operation.READ);
			given(calenderReadService.readCalenderDates(any(Member.class), anyLong(), anyLong(),
				anyInt(), anyInt()))
				.willThrow(exception);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L)
					.param("coupleId", "1")
					.param("year", String.valueOf(LocalDate.now().getYear()))
					.param("month", String.valueOf(LocalDate.now().getMonthValue())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(exception.getErrorCode().getCode()))
				.andExpect(jsonPath("$.message").value(exception.getMessage()));
		}

		@DisplayName("[실패] 현재 로그인한 회원이 연결된 커플의 id와 요청의 coupleId가 다르면 실패한다.")
		@Test
		void should_throwNoPermissionException_When_mismatchCoupleId() throws Exception {

			// Stubbing
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.READ);
			given(calenderReadService.readCalenderDates(any(Member.class), anyLong(), anyLong(),
				anyInt(), anyInt()))
				.willThrow(exception);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L)
					.param("coupleId", "1")
					.param("year", String.valueOf(LocalDate.now().getYear()))
					.param("month", String.valueOf(LocalDate.now().getMonthValue())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(exception.getErrorCode().getCode()))
				.andExpect(jsonPath("$.message").value(exception.getMessage()));
		}

		@DisplayName("[실패] coupleId, year 또는 month 올바르지 않은 값이 들어가면 실패한다.")
		@ParameterizedTest
		@CsvSource({"aa,2023,7", "1,aa,7", "1,2023,aa"})
		void failWithInvalidQueryParameter(String coupleId, String year, String month) throws Exception {

			CalenderDateServiceResponse response = CalenderDateServiceResponse.builder()
				.schedules(List.of(
					CalenderEntry.builder()
						.date(LocalDate.of(2023, 7, 13))
						.events(List.of("anniversary", "mySchedule"))
						.build(),
					CalenderEntry.builder()
						.date(LocalDate.of(2023, 7, 15))
						.events(List.of("anniversary", "partnerSchedule"))
						.build(),
					CalenderEntry.builder()
						.date(LocalDate.of(2023, 7, 13))
						.events(List.of("anniversary", "datingSchedule"))
						.build()
				))
				.build();

			// Stubbing
			given(calenderReadService.readCalenderDates(any(Member.class), anyLong(), anyLong(),
				anyInt(), anyInt()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1)
					.param("coupleId", coupleId)
					.param("year", year)
					.param("month", month))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value("C003")
				);
		}
	}

	private Member createMember() {
		return Member.builder()
			.phone("01012345678")
			.password("password")
			.name("name")
			.nickname("nickname")
			.birthDay(LocalDate.of(2010, 10, 10))
			.gender(Gender.MALE)
			.build();
	}
}
