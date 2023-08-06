package com.dateplan.dateplan.controller.schedule;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DATE_TIME_RANGE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_REPEAT_END_TIME_RANGE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_REPEAT_RULE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_CONTENT;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_LOCATION;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_TITLE;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_INPUT_VALUE;
import static com.dateplan.dateplan.global.exception.ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.controller.dto.request.ScheduleRequest;
import com.dateplan.dateplan.domain.schedule.controller.dto.request.ScheduleUpdateRequest;
import com.dateplan.dateplan.domain.schedule.controller.dto.response.ScheduleEntry;
import com.dateplan.dateplan.domain.schedule.service.dto.request.ScheduleServiceRequest;
import com.dateplan.dateplan.domain.schedule.service.dto.request.ScheduleUpdateServiceRequest;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleDatesServiceResponse;
import com.dateplan.dateplan.domain.schedule.service.dto.response.ScheduleServiceResponse;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.global.exception.schedule.InvalidDateTimeRangeException;
import com.dateplan.dateplan.global.exception.schedule.ScheduleNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

public class ScheduleControllerTest extends ControllerTestSupport {

	@BeforeEach
	void setUp() throws Exception {
		given(authInterceptor.preHandle(
			any(HttpServletRequest.class),
			any(HttpServletResponse.class),
			any(Object.class)))
			.willReturn(true);

		given(scheduleAuthInterceptor.preHandle(
			any(HttpServletRequest.class),
			any(HttpServletResponse.class),
			any(Object.class)))
			.willReturn(true);
	}

	@Nested
	@DisplayName("개인 일정을 생성할 때")
	class CreateSchedule {

		@BeforeEach
		void setUp() {
			Member member = createMember();
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		private final static String REQUEST_URL = "/api/members/{member_id}/schedules";

		@DisplayName("올바른 멤버 id와, 일정 정보를 입력하면 성공한다.")
		@Test
		void successWithValidRequest() throws Exception {

			// Given
			ScheduleRequest request = createScheduleRequest();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(any(Member.class), any(ScheduleServiceRequest.class));

			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("반복 종료일자가 유효하지 않으면 실패한다.")
		@ParameterizedTest
		@CsvSource({"2050-01-01", "2010-01-01"})
		void failWithInvalidRepeatEndTime(LocalDate repeatEndDate) throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().plusDays(5))
				.location("location")
				.content("content")
				.repeatRule(RepeatRule.M)
				.repeatEndTime(repeatEndDate)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(any(Member.class), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(
					jsonPath("$.code").value(ErrorCode.INVALID_REPEAT_END_TIME_RANGE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_REPEAT_END_TIME_RANGE));
		}

		@DisplayName("일정 종료일자가 일정 시작일자보다 앞서면 실패한다")
		@Test
		void failWithInvalidDateTimeRange() throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().minusDays(1))
				.location("location")
				.content("content")
				.repeatRule(RepeatRule.M)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(any(Member.class), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(ErrorCode.INVALID_DATE_TIME_RANGE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_DATE_TIME_RANGE));
		}

		@DisplayName("올바르지 않은 제목을 입력하면 실패한다.")
		@Test
		void failWithInvalidTitle() throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title(createLengthString(16))
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().minusDays(1))
				.location("location")
				.content("content")
				.repeatRule(RepeatRule.M)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(any(Member.class), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_SCHEDULE_TITLE));
		}

		@DisplayName("올바르지 않은 지역을 입력하면 실패한다.")
		@Test
		void failWithInvalidLocation() throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().minusDays(1))
				.location(createLengthString(21))
				.content("content")
				.repeatRule(RepeatRule.M)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(any(Member.class), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_SCHEDULE_LOCATION));
		}

		@DisplayName("올바르지 않은 일정 내용을 입력하면 실패한다.")
		@Test
		void failWithInvalidContent() throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().minusDays(1))
				.location(createLengthString(81))
				.content("content")
				.repeatRule(RepeatRule.M)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(any(Member.class), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_SCHEDULE_LOCATION));
		}

		@DisplayName("올바르지 않은 반복 패턴을 입력하면 실패한다.")
		@ParameterizedTest
		@CsvSource({"A", "B", "NDWMY", "가나다", "1", "!@"})
		void failWithInvalidRepeatRule(String rule) throws Exception {

			// Given
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("title", "title");
			requestBody.put("startDateTime", LocalDateTime.now());
			requestBody.put("endDateTime", LocalDateTime.now().minusDays(1));
			requestBody.put("repeatRule", rule);

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(any(Member.class), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(requestBody))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_REPEAT_RULE));
		}
	}

	@Nested
	@DisplayName("일정 날짜 조회 시")
	class ReadScheduleDates {

		@BeforeEach
		void setUp() {
			Member member = createMember();
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		private final static String REQUEST_URL = "/api/members/{member_id}/schedules/dates";

		@DisplayName("올바른 member_id, year, month를 입력하면 성공한다.")
		@Test
		void successWithValidRequest() throws Exception {

			// Given
			ScheduleDatesServiceResponse response = createScheduleDatesServiceResponse();
			List<String> expectedDates = response.getScheduleDates().stream()
				.map(LocalDate::toString).toList();

			// Stubbing
			given(scheduleReadService.readScheduleDates(anyLong(), anyInt(), anyInt()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L)
					.param("year", String.valueOf(LocalDate.now().getYear()))
					.param("month", String.valueOf(LocalDate.now().getMonthValue())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(jsonPath("$.data.scheduleDates")
					.value(containsInAnyOrder(expectedDates.toArray()))
				);
		}

		@DisplayName("현재 로그인한 회원이 연결되어 있지 않다면 실패한다.")
		@Test
		void failWithNotConnectedRequest() throws Exception {

			// Stubbing
			given(scheduleReadService.readScheduleDates(anyLong(), anyInt(), anyInt()))
				.willThrow(new MemberNotConnectedException());

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L)
					.param("year", String.valueOf(LocalDate.now().getYear()))
					.param("month", String.valueOf(LocalDate.now().getMonthValue())))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.code").value(ErrorCode.MEMBER_NOT_CONNECTED.getCode()),
					jsonPath("$.message").value(DetailMessage.Member_NOT_CONNECTED)
				);
		}

		@DisplayName("year과 month에 올바르지 않은 값이 들어가면 실패한다.")
		@ParameterizedTest
		@CsvSource({"aaaa,12", "2019,aa"})
		void failWithInvalidQueryParameter(String year, String month) throws Exception {
			// Given
			Map<String, String> requestMap = Map.of("year", year, "month", month);
			ScheduleDatesServiceResponse response = createScheduleDatesServiceResponse();

			// Stubbing
			given(scheduleReadService.readScheduleDates(anyLong(), anyInt(), anyInt()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1)
					.param("year", requestMap.get("year"))
					.param("month", requestMap.get("month")))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value("C003"),
					jsonPath("$.message").value(containsString("Integer"))
				);
		}
	}

	@Nested
	@DisplayName("일정 상세 조회 시")
	class ReadSchedules {

		@BeforeEach
		void setUp() {
			MemberThreadLocal.set(createMember());
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		private static final String REQUEST_URL = "/api/members/{member_id}/schedules";

		@DisplayName("올바른 member_id, year, month, day를 입력하면 성공한다")
		@Test
		void successWithValidRequest() throws Exception {
			// Given
			ScheduleServiceResponse response = createScheduleServiceResponse();
			LocalDate now = LocalDate.now();

			// Stubbing
			given(scheduleReadService.readSchedules(anyLong(), anyInt(), anyInt(), anyInt()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1)
					.param("year", String.valueOf(now.getYear()))
					.param("month", String.valueOf(now.getMonthValue()))
					.param("day", String.valueOf(now.getDayOfMonth())))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.data.schedules[0].scheduleId").value(
						response.getSchedules().get(0).getScheduleId()),
					jsonPath("$.data.schedules[0].title").value(
						response.getSchedules().get(0).getTitle()),
					jsonPath("$.data.schedules[0].content").value(
						response.getSchedules().get(0).getContent()),
					jsonPath("$.data.schedules[0].location").value(
						response.getSchedules().get(0).getLocation()),
					jsonPath("$.data.schedules[0].repeatRule").value(
						response.getSchedules().get(0).getRepeatRule().toString())
				);
		}

		@DisplayName("year, month, day에 올바르지 않은 데이터가 들어가면 실패한다")
		@ParameterizedTest
		@CsvSource({"aaaa,10,10", "2010,aa,10", "2010,10,aa"})
		void failWithInvalidQueryParameter(String year, String month, String day) throws Exception {
			// Given
			Map<String, String> requestMap = Map.of("year", year, "month", month, "day", day);
			ScheduleServiceResponse response = createScheduleServiceResponse();

			// Stubbing
			given(scheduleReadService.readSchedules(anyLong(), anyInt(), anyInt(), anyInt()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1)
					.param("year", requestMap.get("year"))
					.param("month", requestMap.get("month"))
					.param("day", requestMap.get("day")))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value("C003"),
					jsonPath("$.message").value(containsString("Integer"))
				);
		}

		@DisplayName("현재 로그인한 회원이 연결되어 있지 않다면 실패한다.")
		@Test
		void failWithNotConnected() throws Exception {

			// Stubbing
			MemberNotConnectedException exception = new MemberNotConnectedException();
			given(scheduleReadService.readSchedules(anyLong(), anyInt(), anyInt(), anyInt()))
				.willThrow(exception);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L)
					.param("year", String.valueOf(LocalDate.now().getYear()))
					.param("month", String.valueOf(LocalDate.now().getMonthValue()))
					.param("day", String.valueOf(LocalDate.now().getDayOfMonth())))
				.andExpect(status().is(exception.getErrorCode().getHttpStatusCode().value()))
				.andExpectAll(
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}
	}

	@DisplayName("일정 수정 시")
	@Nested
	class UpdateSchedule {

		private static final String REQUEST_URL = "/api/members/{member_id}/schedules/{schedule_id}";

		@BeforeEach
		void setUp() {
			MemberThreadLocal.set(createMember());
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("올바른 member_id, schedule_id, requestDTO를 입력하면 성공한다")
		@Test
		void successWithValidRequest() throws Exception {

			// Given
			ScheduleUpdateRequest request = createScheduleUpdateRequest();

			// Stubbing
			willDoNothing()
				.given(scheduleService)
				.updateSchedule(anyLong(), any(ScheduleUpdateServiceRequest.class),
					any(Member.class), anyBoolean());

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.param("updateRepeat", "true")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.success").value(true)
				);
		}

		@DisplayName("요청에 해당하는 schedule이 존재하지 않으면 실패한다")
		@Test
		void failWithScheduleNotFoundException() throws Exception {

			// Given
			ScheduleUpdateRequest request = createScheduleUpdateRequest();
			ScheduleNotFoundException exception = new ScheduleNotFoundException();

			// Stubbing
			willThrow(exception)
				.given(scheduleService)
				.updateSchedule(anyLong(), any(ScheduleUpdateServiceRequest.class),
					any(Member.class), anyBoolean());

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.param("updateRepeat", "true")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isNotFound(),
					jsonPath("$.success").value(false),
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}

		@DisplayName("일정 종료가 일정 시작보다 앞서면 실패한다")
		@Test
		void failWithInvalidStartDateTime() throws Exception {

			// Given
			ScheduleUpdateRequest request = createScheduleUpdateRequest();
			InvalidDateTimeRangeException exception = new InvalidDateTimeRangeException();

			// Stubbing
			willThrow(exception)
				.given(scheduleService)
				.updateSchedule(anyLong(), any(ScheduleUpdateServiceRequest.class),
					any(Member.class), anyBoolean());

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.param("updateRepeat", "true")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value(false),
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}

		@DisplayName("일정 종료일시가 유효한 범위가 아니면 실패한다")
		@Test
		void failWithInvalidEndDateTime() throws Exception {

			// Given
			ScheduleUpdateRequest request = ScheduleUpdateRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.of(2050, 1, 1, 0, 0))
				.location("location")
				.content("content")
				.build();

			// Stubbing
			willDoNothing()
				.given(scheduleService)
				.updateSchedule(anyLong(), any(ScheduleUpdateServiceRequest.class),
					any(Member.class), anyBoolean());

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.param("updateRepeat", "true")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value(false),
					jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_CALENDER_TIME_RANGE)
				);
		}

		@DisplayName("올바르지 않은 제목을 입력하면 실패한다.")
		@Test
		void failWithInvalidTitle() throws Exception {

			// Given
			ScheduleUpdateRequest request = ScheduleUpdateRequest.builder()
				.title(createLengthString(16))
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().plusDays(5))
				.location("location")
				.content("content")
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.updateSchedule(anyLong(), any(ScheduleUpdateServiceRequest.class),
					any(Member.class), anyBoolean());

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.param("updateRepeat", "true")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_SCHEDULE_TITLE));
		}

		@DisplayName("올바르지 않은 지역을 입력하면 실패한다.")
		@Test
		void failWithInvalidLocation() throws Exception {

			// Given
			ScheduleUpdateRequest request = ScheduleUpdateRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().plusDays(5))
				.location(createLengthString(21))
				.content("content")
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.updateSchedule(anyLong(), any(ScheduleUpdateServiceRequest.class),
					any(Member.class), anyBoolean());

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.param("updateRepeat", "true")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_SCHEDULE_LOCATION));
		}

		@DisplayName("올바르지 않은 일정 내용을 입력하면 실패한다.")
		@Test
		void failWithInvalidContent() throws Exception {

			// Given
			ScheduleUpdateRequest request = ScheduleUpdateRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().plusDays(5))
				.location("location")
				.content(createLengthString(81))
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.updateSchedule(anyLong(), any(ScheduleUpdateServiceRequest.class),
					any(Member.class), anyBoolean());

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.param("updateRepeat", "true")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_SCHEDULE_CONTENT));
		}

		@DisplayName("request param의 타입이 올바르지 않으면 실패한다.")
		@Test
		void failWithInvalidRequestParam() throws Exception {

			// Given
			ScheduleUpdateRequest request = createScheduleUpdateRequest();

			// Stubbing
			willDoNothing()
				.given(scheduleService)
				.updateSchedule(anyLong(), any(ScheduleUpdateServiceRequest.class),
					any(Member.class), anyBoolean());

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.param("updateRepeat", "asdf")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value(false),
					jsonPath("$.code").value(METHOD_ARGUMENT_TYPE_MISMATCH.getCode()),
					jsonPath("$.message").value(containsString("updateRepeat"))
				);
		}
	}

	@Nested
	@DisplayName("일정 삭제 시")
	class DeleteSchedule {

		private static final String REQUEST_URL = "/api/members/{member_id}/schedules/{schedule_id}";

		@BeforeEach
		void setUp() {
			MemberThreadLocal.set(createMember());
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("올바른 memberId, scheduleId를 입력하면 일정이 삭제된다.")
		@Test
		void successWithValidRequest() throws Exception {

			// Stubbing
			willDoNothing()
				.given(scheduleService)
				.deleteSchedule(anyLong(), anyBoolean());

			// When & Then
			mockMvc.perform(
					delete(REQUEST_URL, 1, 1)
						.param("deleteRepeat", "true"))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.success").value("true")
				);
		}

		@DisplayName("request param의 타입이 올바르지 않으면 실패한다.")
		@Test
		void failWithInvalidRequestParam() throws Exception {

			// Stubbing
			willDoNothing()
				.given(scheduleService)
				.deleteSchedule(anyLong(), anyBoolean());

			// When & Then
			mockMvc.perform(
					delete(REQUEST_URL, 1, 1)
						.param("deleteRepeat", "asdf"))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value(false),
					jsonPath("$.code").value(METHOD_ARGUMENT_TYPE_MISMATCH.getCode()),
					jsonPath("$.message").value(containsString("deleteRepeat"))
				);
		}

		@DisplayName("요청에 해당하는 일정이 존재하지 않으면 실패한다")
		@Test
		void failWithScheduleNotFound() throws Exception {

			// Stubbing
			ScheduleNotFoundException exception = new ScheduleNotFoundException();
			willThrow(exception)
				.given(scheduleService)
				.deleteSchedule(anyLong(), anyBoolean());

			// When & Then
			mockMvc.perform(
					delete(REQUEST_URL, 1, 1)
						.param("deleteRepeat", "true"))
				.andExpectAll(
					status().isNotFound(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}
	}

	private ScheduleRequest createScheduleRequest() {
		return ScheduleRequest.builder()
			.title("title")
			.startDateTime(LocalDateTime.now())
			.endDateTime(LocalDateTime.now().plusDays(5))
			.location("location")
			.content("content")
			.repeatRule(RepeatRule.M)
			.repeatEndTime(LocalDate.now().plusYears(10))
			.build();
	}

	private String createLengthString(int length) {
		return new String(new char[length]).replace('\0', ' ');
	}

	private ScheduleDatesServiceResponse createScheduleDatesServiceResponse() {
		return ScheduleDatesServiceResponse.builder()
			.scheduleDates(createScheduleDates())
			.build();
	}

	private List<LocalDate> createScheduleDates() {
		return LocalDate.now().withDayOfMonth(1)
			.datesUntil(LocalDate.now().withDayOfMonth(1).plusMonths(1))
			.collect(Collectors.toList());
	}

	private ScheduleServiceResponse createScheduleServiceResponse() {
		return ScheduleServiceResponse.builder()
			.schedules(createScheduleEntries())
			.build();
	}

	private List<ScheduleEntry> createScheduleEntries() {
		return IntStream.rangeClosed(1, 5)
			.mapToObj(i -> ScheduleEntry.builder()
				.scheduleId((long) i)
				.title("title")
				.repeatRule(RepeatRule.N)
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().plusDays(5))
				.build())
			.toList();
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

	private ScheduleUpdateRequest createScheduleUpdateRequest() {
		return ScheduleUpdateRequest.builder()
			.title("newTitle")
			.startDateTime(LocalDateTime.now())
			.endDateTime(LocalDateTime.now().plusDays(5))
			.content("newContent")
			.location("newLocation")
			.build();
	}
}
