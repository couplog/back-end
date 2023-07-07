package com.dateplan.dateplan.controller.dating;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.dating.controller.dto.request.DatingCreateRequest;
import com.dateplan.dateplan.domain.dating.controller.dto.request.DatingUpdateRequest;
import com.dateplan.dateplan.domain.dating.service.dto.request.DatingCreateServiceRequest;
import com.dateplan.dateplan.domain.dating.service.dto.request.DatingUpdateServiceRequest;
import com.dateplan.dateplan.domain.dating.service.dto.response.DatingDatesServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.global.exception.dating.DatingNotFoundException;
import com.dateplan.dateplan.global.exception.schedule.InvalidDateTimeRangeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

public class DatingControllerTest extends ControllerTestSupport {

	@BeforeEach
	void setUp() {
		given(
			authInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class),
				any(Object.class)))
			.willReturn(true);
	}

	@Nested
	@DisplayName("데이트 일정 생성 시")
	class CreateDating {

		private static final String REQUEST_URL = "/api/couples/{couple_id}/dating";

		@BeforeEach
		void setUp() {
			MemberThreadLocal.set(createMember());
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("올바른 요청 DTO, couple_id를 입력하면 데이트 일정이 생성된다.")
		@Test
		void successWithValidRequest() throws Exception {

			// Given
			DatingCreateRequest request = createDatingCreateRequest(null, null, null, null, null);

			// Stubbing
			willDoNothing()
				.given(datingService)
				.createDating(any(Member.class), anyLong(), any(DatingCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(
				post(REQUEST_URL, 1)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8)
			).andExpectAll(
				status().isCreated(),
				jsonPath("$.success").value("true")
			);
		}

		@DisplayName("입력의 제목이 유효하지 않으면 실패한다")
		@Test
		void failWithInvalidTitle() throws Exception {

			// Given
			DatingCreateRequest request = createDatingCreateRequest("1".repeat(16), null, null,
				null, null);

			// Stubbing
			willDoNothing()
				.given(datingService)
				.createDating(any(Member.class), anyLong(), any(DatingCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(
				post(REQUEST_URL, 1)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8)
			).andExpectAll(
				status().isBadRequest(),
				jsonPath("$.success").value("false"),
				jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()),
				jsonPath("$.message").value(DetailMessage.INVALID_SCHEDULE_TITLE)
			);
		}

		@DisplayName("입력의 내용이 유효하지 않으면 실패한다")
		@Test
		void failWithInvalidContent() throws Exception {

			// Given
			DatingCreateRequest request = createDatingCreateRequest(null, "1".repeat(81), null,
				null, null);

			// Stubbing
			willDoNothing()
				.given(datingService)
				.createDating(any(Member.class), anyLong(), any(DatingCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(
				post(REQUEST_URL, 1)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8)
			).andExpectAll(
				status().isBadRequest(),
				jsonPath("$.success").value("false"),
				jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()),
				jsonPath("$.message").value(DetailMessage.INVALID_SCHEDULE_CONTENT)
			);
		}

		@DisplayName("입력의 위치가 유효하지 않으면 실패한다")
		@Test
		void failWithInvalidLocation() throws Exception {

			// Given
			DatingCreateRequest request = createDatingCreateRequest(null, null, "1".repeat(21),
				null, null);

			// Stubbing
			willDoNothing()
				.given(datingService)
				.createDating(any(Member.class), anyLong(), any(DatingCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(
				post(REQUEST_URL, 1)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8)
			).andExpectAll(
				status().isBadRequest(),
				jsonPath("$.success").value("false"),
				jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()),
				jsonPath("$.message").value(DetailMessage.INVALID_SCHEDULE_LOCATION)
			);
		}

		@DisplayName("요청한 couple_id와 현재 로그인한 멤버의 couple_id가 다르면 실패한다")
		@Test
		void failWithNoPermission() throws Exception {

			// Given
			DatingCreateRequest request = createDatingCreateRequest(null, null, null, null, null);

			// Stubbing
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.CREATE);
			willThrow(exception)
				.given(datingService)
				.createDating(any(Member.class), anyLong(), any(DatingCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(
				post(REQUEST_URL, 1)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8)
			).andExpectAll(
				status().isForbidden(),
				jsonPath("$.success").value("false"),
				jsonPath("$.code").value(exception.getErrorCode().getCode()),
				jsonPath("$.message").value(exception.getMessage())
			);
		}

		@DisplayName("pathVariable의 타입이 유효하지 않으면 실패한다")
		@Test
		void failWithInvalidTypeMismatch() throws Exception {

			// Given
			DatingCreateRequest request = createDatingCreateRequest(null, null, null, null, null);

			// Stubbing
			willDoNothing()
				.given(datingService)
				.createDating(any(Member.class), anyLong(), any(DatingCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(
				post(REQUEST_URL, "abc")
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8)
			).andExpectAll(
				status().isBadRequest(),
				jsonPath("$.success").value("false"),
				jsonPath("$.code").value(ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.getCode()),
				jsonPath("$.message").value(containsString("Long"))
			);
		}

		@DisplayName("데이트 일정 종료 시간이 2049년 12월 31일 이후이면 실패한다")
		@Test
		void failWithInvalidCalendarEndDate() throws Exception {

			// Given
			DatingCreateRequest request = createDatingCreateRequest(null, null, null, null,
				LocalDateTime.of(2050, 1, 1, 0, 0));

			// Stubbing
			willDoNothing()
				.given(datingService)
				.createDating(any(Member.class), anyLong(), any(DatingCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(
				post(REQUEST_URL, 1)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8)
			).andExpectAll(
				status().isBadRequest(),
				jsonPath("$.success").value("false"),
				jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()),
				jsonPath("$.message").value(DetailMessage.INVALID_CALENDER_TIME_RANGE)
			);
		}

		@DisplayName("일정 시작 시간이 일정 종료시간 이후이면 실패한다")
		@Test
		void failWithInvalidDateTimeRange() throws Exception {

			// Given
			DatingCreateRequest request = createDatingCreateRequest(null, null, null,
				LocalDateTime.now(),
				LocalDateTime.now().minusDays(1));

			// Stubbing
			willDoNothing()
				.given(datingService)
				.createDating(any(Member.class), anyLong(), any(DatingCreateServiceRequest.class));

			// When & Then
			InvalidDateTimeRangeException exception = new InvalidDateTimeRangeException();

			mockMvc.perform(
				post(REQUEST_URL, 1)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8)
			).andExpectAll(
				status().isBadRequest(),
				jsonPath("$.success").value("false"),
				jsonPath("$.code").value(exception.getErrorCode().getCode()),
				jsonPath("$.message").value(exception.getMessage())
			);
		}

		@DisplayName("회원이 연결되어있지 않으면 실패한다")
		@Test
		void failWithMemberNotConnected() throws Exception {

			// Given
			DatingCreateRequest request = createDatingCreateRequest(null, null, null, null, null);

			// Stubbing
			MemberNotConnectedException exception = new MemberNotConnectedException();
			willThrow(exception)
				.given(datingService)
				.createDating(any(Member.class), anyLong(), any(DatingCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(
				post(REQUEST_URL, 1)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8)
			).andExpectAll(
				status().isBadRequest(),
				jsonPath("$.success").value("false"),
				jsonPath("$.code").value(exception.getErrorCode().getCode()),
				jsonPath("$.message").value(exception.getMessage())
			);
		}
	}

	@Nested
	@DisplayName("데이트 일정 날짜 조회 시")
	class ReadDatingDates {

		private final static String REQUEST_URL = "/api/couples/{couple_id}/dating/dates";

		@BeforeEach
		void setUp() {
			MemberThreadLocal.set(createMember());
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("올바른 couple_id, year, month를 입력하면 성공한다.")
		@Test
		void successWithValidRequest() throws Exception {

			// Given
			DatingDatesServiceResponse response = createDatingDatesServiceResponse();
			List<String> expectedDates = response.getDatingDates().stream()
				.map(LocalDate::toString).toList();

			// Stubbing
			given(
				datingReadService.readDatingDates(any(Member.class), anyLong(), anyInt(), anyInt()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L)
					.param("year", String.valueOf(LocalDate.now().getYear()))
					.param("month", String.valueOf(LocalDate.now().getMonthValue())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(jsonPath("$.data.datingDates")
					.value(containsInAnyOrder(expectedDates.toArray()))
				);
		}

		@DisplayName("현재 로그인한 회원이 연결된 커플의 couple_id와 요청의 couple_id가 다르면 실패한다.")
		@Test
		void failWithNoPermissionRequest() throws Exception {

			// Stubbing
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.READ);
			given(
				datingReadService.readDatingDates(any(Member.class), anyLong(), anyInt(), anyInt()))
				.willThrow(exception);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L)
					.param("year", String.valueOf(LocalDate.now().getYear()))
					.param("month", String.valueOf(LocalDate.now().getMonthValue())))
				.andExpect(status().isForbidden())
				.andExpectAll(
					jsonPath("$.code").value(ErrorCode.NO_PERMISSION.getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}

		@DisplayName("현재 로그인한 회원이 연결되어 있지 않다면 실패한다.")
		@Test
		void failWithNotConnectedRequest() throws Exception {

			// Stubbing
			MemberNotConnectedException exception = new MemberNotConnectedException();
			given(
				datingReadService.readDatingDates(any(Member.class), anyLong(), anyInt(), anyInt()))
				.willThrow(exception);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L)
					.param("year", String.valueOf(LocalDate.now().getYear()))
					.param("month", String.valueOf(LocalDate.now().getMonthValue())))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}

		@DisplayName("year과 month에 올바르지 않은 값이 들어가면 실패한다.")
		@ParameterizedTest
		@CsvSource({"aaaa,12", "2019,aa"})
		void failWithInvalidQueryParameter(String year, String month) throws Exception {
			// Given
			Map<String, String> requestMap = Map.of("year", year, "month", month);
			DatingDatesServiceResponse response = createDatingDatesServiceResponse();

			// Stubbing
			given(
				datingReadService.readDatingDates(any(Member.class), anyLong(), anyInt(), anyInt()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1)
					.param("year", requestMap.get("year"))
					.param("month", requestMap.get("month")))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.getCode()),
					jsonPath("$.message").value(containsString("Integer"))
				);
		}
	}

	@Nested
	@DisplayName("데이트 일정 수정 시")
	class UpdateDating {

		public static final String REQUEST_URL = "/api/couples/{couple_id}/dating/{dating_id}";

		@BeforeEach
		void setUp() {
			MemberThreadLocal.set(createMember());
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@Test
		void 성공_올바른커플id와_데이트id와_DTO를요청하면_성공한다() throws Exception {
			DatingUpdateRequest request = createDatingUpdateRequest(null, null, null, null, null);

			willDoNothing()
				.given(datingService)
				.updateDating(any(Member.class), anyLong(), anyLong(),
					any(DatingUpdateServiceRequest.class));

			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.success").value("true")
				);
		}

		@Test
		void 실패_제목이15자가넘어가면_예외를반환한다() throws Exception {
			DatingUpdateRequest request = createDatingUpdateRequest("1".repeat(16), null, null,
				null, null);

			willDoNothing()
				.given(datingService)
				.updateDating(any(Member.class), anyLong(), anyLong(),
					any(DatingUpdateServiceRequest.class));

			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_SCHEDULE_TITLE)
				);
		}

		@Test
		void 실패_위치가_20자가넘어가면_예외를반환한다() throws Exception {
			DatingUpdateRequest request = createDatingUpdateRequest(null, null, "1".repeat(21),
				null, null);

			willDoNothing()
				.given(datingService)
				.updateDating(any(Member.class), anyLong(), anyLong(),
					any(DatingUpdateServiceRequest.class));

			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_SCHEDULE_LOCATION)
				);
		}

		@Test
		void 실패_내용이_80자가넘어가면_예외를반환한다() throws Exception {
			DatingUpdateRequest request = createDatingUpdateRequest(null, "1".repeat(81), null,
				null, null);

			willDoNothing()
				.given(datingService)
				.updateDating(any(Member.class), anyLong(), anyLong(),
					any(DatingUpdateServiceRequest.class));

			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_SCHEDULE_CONTENT)
				);
		}

		@Test
		void 실패_pathVariable에_올바른값이들어가지않으면_예외를반환한다() throws Exception {
			DatingUpdateRequest request = createDatingUpdateRequest(null, null, null, null, null);

			willDoNothing()
				.given(datingService)
				.updateDating(any(Member.class), anyLong(), anyLong(),
					any(DatingUpdateServiceRequest.class));

			mockMvc.perform(
					put(REQUEST_URL, "a", "b")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.getCode()),
					jsonPath("$.message").value(containsString("Long"))
				);
		}

		@Test
		void 실패_요청한회원이연결된couple의id와_요청의coupleId가다르면_예외를반환한다() throws Exception {
			DatingUpdateRequest request = createDatingUpdateRequest(null, null, null, null, null);

			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.UPDATE);
			willThrow(exception)
				.given(datingService)
				.updateDating(any(Member.class), anyLong(), anyLong(),
					any(DatingUpdateServiceRequest.class));

			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isForbidden(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}

		@Test
		void 실패_요청에해당하는데이트일정이_존재하지않으면_예외를반환한다() throws Exception {
			DatingUpdateRequest request = createDatingUpdateRequest(null, null, null, null, null);

			DatingNotFoundException exception = new DatingNotFoundException();
			willThrow(exception)
				.given(datingService)
				.updateDating(any(Member.class), anyLong(), anyLong(),
					any(DatingUpdateServiceRequest.class));

			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isNotFound(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}

		@Test
		void 실패_종료시간이_시작시간보다앞서면_예외를반환한다() throws Exception {
			DatingUpdateRequest request = createDatingUpdateRequest(null, null, null,
				LocalDateTime.now(), LocalDateTime.now().minusDays(1));

			willDoNothing()
				.given(datingService)
				.updateDating(any(Member.class), anyLong(), anyLong(),
					any(DatingUpdateServiceRequest.class));

			InvalidDateTimeRangeException exception = new InvalidDateTimeRangeException();
			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}

		@Test
		void 실패_종료시간이_2049년12월31일이후이면_예외를반환한다() throws Exception {
			DatingUpdateRequest request = createDatingUpdateRequest(null, null, null,
				null, LocalDateTime.of(2050, 1, 1, 0, 0));

			willDoNothing()
				.given(datingService)
				.updateDating(any(Member.class), anyLong(), anyLong(),
					any(DatingUpdateServiceRequest.class));

			mockMvc.perform(
					put(REQUEST_URL, 1, 1)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpectAll(
					status().isBadRequest(),
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_CALENDER_TIME_RANGE)
				);
		}
	}

	private static Member createMember() {
		return Member.builder()
			.name("name")
			.phone("01012345678")
			.gender(Gender.MALE)
			.nickname("nickname")
			.birthDay(LocalDate.of(2010, 10, 10))
			.password("password")
			.build();
	}

	private DatingCreateRequest createDatingCreateRequest(
		String title,
		String content,
		String location,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime
	) {
		return DatingCreateRequest.builder()
			.title(title == null ? "title" : title)
			.content(content == null ? "content" : content)
			.location(location == null ? "location" : location)
			.startDateTime(startDateTime == null ?
				LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES) : startDateTime)
			.endDateTime(endDateTime == null ?
				LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.MINUTES) : endDateTime)
			.build();
	}

	public DatingDatesServiceResponse createDatingDatesServiceResponse() {
		return DatingDatesServiceResponse.builder()
			.datingDates(createDatingDates())
			.build();
	}

	private List<LocalDate> createDatingDates() {
		return LocalDate.now().withDayOfMonth(1)
			.datesUntil(LocalDate.now().withDayOfMonth(1).plusMonths(1))
			.collect(Collectors.toList());
	}

	private DatingUpdateRequest createDatingUpdateRequest(
		String title,
		String content,
		String location,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime
	) {
		return DatingUpdateRequest.builder()
			.title(title == null ? "title" : title)
			.content(content == null ? "content" : content)
			.location(location == null ? "location" : location)
			.startDateTime(startDateTime == null ?
				LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES) : startDateTime)
			.endDateTime(endDateTime == null ?
				LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.MINUTES) : endDateTime)
			.build();
	}
}
