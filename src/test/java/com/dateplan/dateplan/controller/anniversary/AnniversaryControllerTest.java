package com.dateplan.dateplan.controller.anniversary;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_ANNIVERSARY_CONTENT;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_ANNIVERSARY_REPEAT_RULE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_ANNIVERSARY_TITLE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CALENDER_TIME_RANGE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DATE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_INPUT_VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.anniversary.controller.dto.request.AnniversaryCreateRequest;
import com.dateplan.dateplan.domain.anniversary.controller.dto.request.AnniversaryModifyRequest;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryCategory;
import com.dateplan.dateplan.domain.anniversary.entity.AnniversaryRepeatRule;
import com.dateplan.dateplan.domain.anniversary.service.dto.request.AnniversaryCreateServiceRequest;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryDatesServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryListServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.AnniversaryServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.ComingAnniversaryListServiceResponse;
import com.dateplan.dateplan.domain.anniversary.service.dto.response.ComingAnniversaryServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.http.MediaType;

public class AnniversaryControllerTest extends ControllerTestSupport {

	@BeforeEach
	void setUp() throws Exception {
		given(
			authInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class),
				any(Object.class)))
			.willReturn(true);

		given(anniversaryAuthInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
			.willReturn(true);
	}

	@Nested
	@DisplayName("기념일 생성시")
	class CreateAnniversary {

		private static final String REQUEST_URL = "/api/couples/{couple_id}/anniversary";

		@BeforeEach
		void setUp() {
			Member member = createMember();
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("현재 로그인한 회원이 요청한 couple id 의 couple 내에 존재한다면 성공한다.")
		@Test
		void withLoginMemberWithExistsInCouple() throws Exception {

			// Given
			Long coupleId = 1L;
			AnniversaryCreateRequest request = AnniversaryCreateRequest.builder()
				.title("title")
				.content("content")
				.date(LocalDate.of(2020, 10, 10))
				.repeatRule(AnniversaryRepeatRule.YEAR)
				.build();

			// Stub
			willDoNothing()
				.given(anniversaryService)
				.createAnniversaries(anyLong(), any(AnniversaryCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, coupleId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("제목을 입력하지 않거나 2-15자가 아니라면 에러 코드, 메시지를 응답한다.")
		@NullSource
		@CsvSource({"a", "aaaaaaaaaaaaaaaa"})
		@ParameterizedTest
		void withInvalidTitle(String title) throws Exception {

			// Given
			Long coupleId = 1L;
			AnniversaryCreateRequest request = AnniversaryCreateRequest.builder()
				.title(title)
				.content("content")
				.date(LocalDate.of(2020, 10, 10))
				.repeatRule(AnniversaryRepeatRule.YEAR)
				.build();

			// Stub
			willDoNothing()
				.given(anniversaryService)
				.createAnniversaries(anyLong(), any(AnniversaryCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, coupleId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_ANNIVERSARY_TITLE));
		}

		@DisplayName("내용이 81자 이상이라면 에러 코드, 메시지를 응답한다.")
		@Test
		void withInvalidContentLength() throws Exception {

			// Given
			Long coupleId = 1L;
			AnniversaryCreateRequest request = AnniversaryCreateRequest.builder()
				.title("title")
				.content("t".repeat(81))
				.date(LocalDate.of(2020, 10, 10))
				.repeatRule(AnniversaryRepeatRule.YEAR)
				.build();

			// Stub
			willDoNothing()
				.given(anniversaryService)
				.createAnniversaries(anyLong(), any(AnniversaryCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, coupleId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_ANNIVERSARY_CONTENT));
		}

		@DisplayName("반복 패턴이 NONE, YEAR 가 아니라면 에러 코드, 메시지를 응답한다.")
		@NullAndEmptySource
		@CsvSource({"ABC"})
		@ParameterizedTest
		void withInvalidRepeatRule(String repeatRule) throws Exception {

			// Given
			Long coupleId = 1L;
			Map<String, String> request = createAnniversaryCreateRequestMap(repeatRule,
				"2020-10-10");

			// Stub
			willDoNothing()
				.given(anniversaryService)
				.createAnniversaries(anyLong(), any(AnniversaryCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, coupleId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_ANNIVERSARY_REPEAT_RULE));
		}

		@DisplayName("기념일 날짜가 yyyy-MM-dd 패턴이 아니라면 에러 코드, 메시지를 응답한다.")
		@NullAndEmptySource
		@CsvSource({"20201010", "2020/10/10", "2020.10.10"})
		@ParameterizedTest
		void withInvalidDatePattern(String date) throws Exception {

			// Given
			Long coupleId = 1L;
			Map<String, String> request = createAnniversaryCreateRequestMap("NONE", date);

			// Stub
			willDoNothing()
				.given(anniversaryService)
				.createAnniversaries(anyLong(), any(AnniversaryCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, coupleId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_DATE_PATTERN));
		}

		@DisplayName("기념일 날짜가 2049-12-31 이후라면 에러 코드, 메시지를 응답한다.")
		@Test
		void withInvalidDateRange() throws Exception {

			// Given
			Long coupleId = 1L;
			Map<String, String> request = createAnniversaryCreateRequestMap("NONE", "2050-01-01");

			// Stub
			willDoNothing()
				.given(anniversaryService)
				.createAnniversaries(anyLong(), any(AnniversaryCreateServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, coupleId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_CALENDER_TIME_RANGE));
		}
	}

	@Nested
	@DisplayName("기념일 날짜 조회시")
	class ReadAnniversaryDates {

		private static final String REQUEST_URL = "/api/couples/{couple_id}/anniversary/dates";

		@BeforeEach
		void setUp() {
			Member member = createMember();
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("대상 커플의 id 와 현재 로그인 회원의 커플 id 가 같다면, 날짜를 조회한다.")
		@Test
		void withSameValueLoginMemberCoupleIdAndTargetCoupleId() throws Exception {

			// given
			Long coupleId = 1L;
			Integer year = 2023;
			Integer month = 1;

			List<LocalDate> anniversaryDates = List.of(LocalDate.of(2023, 1, 1));
			AnniversaryDatesServiceResponse serviceResponse = createAnniversaryDatesServiceResponse(
				anniversaryDates);

			// stub
			given(
				anniversaryReadService.readAnniversaryDates(anyLong(), anyInt(), anyInt()))
				.willReturn(serviceResponse);

			// when & then
			mockMvc.perform(get(REQUEST_URL, coupleId)
					.queryParam("year", String.valueOf(year))
					.queryParam("month", String.valueOf(month)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(jsonPath("$.data.anniversaryDates[0]").value(
					anniversaryDates.get(0).toString()));
		}
	}

	@Nested
	@DisplayName("기념일 조회시")
	class ReadAnniversaries {

		private static final String REQUEST_URL = "/api/couples/{couple_id}/anniversary";

		@BeforeEach
		void setUp() {
			Member member = createMember();
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("대상 커플의 id 와 현재 로그인 회원의 커플 id 가 같다면, 기념일을 조회한다.")
		@Test
		void withSameValueLoginMemberCoupleIdAndTargetCoupleId() throws Exception {

			// given
			Long coupleId = 1L;
			Integer year = 2023;
			Integer month = 1;
			Integer day = 1;

			AnniversaryListServiceResponse serviceResponse = createAnniversaryListServiceResponse();
			List<AnniversaryServiceResponse> serviceResponses = serviceResponse.getAnniversaries();

			// stub
			given(
				anniversaryReadService.readAnniversaries(anyLong(), anyInt(),
					anyInt(), anyInt(), anyBoolean()))
				.willReturn(serviceResponse);

			// when & then
			mockMvc.perform(get(REQUEST_URL, coupleId)
					.queryParam("year", String.valueOf(year))
					.queryParam("month", String.valueOf(month))
					.queryParam("day", String.valueOf(day)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(
					jsonPath("$.data.anniversaries[0].id").value(serviceResponses.get(0).getId()))
				.andExpect(jsonPath("$.data.anniversaries[0].title").value(
					serviceResponses.get(0).getTitle()))
				.andExpect(jsonPath("$.data.anniversaries[0].content").value(
					serviceResponses.get(0).getContent()))
				.andExpect(jsonPath("$.data.anniversaries[0].repeatRule").value(
					serviceResponses.get(0).getRepeatRule().name()))
				.andExpect(jsonPath("$.data.anniversaries[0].category").value(
					serviceResponses.get(0).getCategory().name()))
				.andExpect(jsonPath("$.data.anniversaries[0].date").value(
					serviceResponses.get(0).getDate().toString()));
		}
	}

	@Nested
	@DisplayName("다가오는 기념일 조회시")
	class ReadComingAnniversaries {

		private static final String REQUEST_URL = "/api/couples/{couple_id}/anniversary/coming";

		@BeforeEach
		void setUp() {
			Member member = createMember();
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("대상 커플의 id 와 현재 로그인 회원의 커플 id 가 같다면, 다가오는 기념일을 조회한다.")
		@Test
		void withSameValueLoginMemberCoupleIdAndTargetCoupleId() throws Exception {

			// given
			Long coupleId = 1L;

			ComingAnniversaryListServiceResponse serviceResponse = createComingAnniversaryListServiceResponse();
			List<ComingAnniversaryServiceResponse> serviceResponses = serviceResponse.getAnniversaries();

			// stub
			given(
				anniversaryReadService.readComingAnniversaries(anyLong(), nullable(LocalDate.class), anyInt()))
				.willReturn(serviceResponse);

			// when & then
			mockMvc.perform(get(REQUEST_URL, coupleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(
					jsonPath("$.data.anniversaries[0].id").value(serviceResponses.get(0).getId()))
				.andExpect(jsonPath("$.data.anniversaries[0].title").value(
					serviceResponses.get(0).getTitle()))
				.andExpect(jsonPath("$.data.anniversaries[0].content").value(
					serviceResponses.get(0).getContent()))
				.andExpect(jsonPath("$.data.anniversaries[0].date").value(
					serviceResponses.get(0).getDate().toString()));
		}

		@DisplayName("잘못된 형식의 날짜를 입력하면 에러 코드, 메시지를 응답한다.")
		@CsvSource({"20201010", "2020/10/10", "2020.10.10"})
		@ParameterizedTest
		void withInvalidDateFormat(String date) throws Exception {

			// given
			Long coupleId = 1L;
			String parameterName = "startDate";

			// when & then
			mockMvc.perform(get(REQUEST_URL, coupleId)
					.queryParam("startDate", date))
				.andExpect(
					status().is(ErrorCode.MISSING_REQUEST_PARAMETER.getHttpStatusCode().value()))
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(
					jsonPath("$.code").value(ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.getCode()))
				.andExpect(jsonPath("$.message").value(
					String.format(DetailMessage.METHOD_ARGUMENT_TYPE_MISMATCH, parameterName,
						LocalDate.class.getSimpleName()))
				);
		}
	}

	@Nested
	@DisplayName("기념일 수정시")
	class ModifyAnniversary {

		private static final String REQUEST_URL = "/api/couples/{couple_id}/anniversary/{anniversary_id}";

		@BeforeEach
		void setUp() {
			Member member = createMember();
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("대상 기념일이 현재 로그인 회원의 커플 기념일이고, 생일, 처음만난날 관련 기념일이 아니며, 입력값이 유효하면 요청에 성공한다.")
		@Test
		void withLoginMembersCouplesAnniversaryAndNotRelatedBirthDayAndFirstDateAndValidInput()
			throws Exception {

			// given
			Long coupleId = 1L;
			Long anniversaryId = 1L;
			AnniversaryModifyRequest request = createAnniversaryModifyRequest(
				"title", "content", LocalDate.of(2020, 10, 10));

			// when & Then
			mockMvc.perform(put(REQUEST_URL, coupleId, anniversaryId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("제목을 입력하지 않거나 2-15자가 아니라면 에러 코드, 메시지를 응답한다.")
		@NullSource
		@CsvSource({"a", "aaaaaaaaaaaaaaaa"})
		@ParameterizedTest
		void withInvalidTitle(String title) throws Exception {

			// Given
			Long coupleId = 1L;
			Long anniversaryId = 1L;
			AnniversaryModifyRequest request = createAnniversaryModifyRequest(
				title, "content", LocalDate.of(2020, 10, 10));

			// When & Then
			mockMvc.perform(put(REQUEST_URL, coupleId, anniversaryId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_ANNIVERSARY_TITLE));
		}

		@DisplayName("내용이 100자 이상이라면 에러 코드, 메시지를 응답한다.")
		@Test
		void withInvalidContentLength() throws Exception {

			// Given
			Long coupleId = 1L;
			Long anniversaryId = 1L;
			AnniversaryModifyRequest request = createAnniversaryModifyRequest(
				"title", "a".repeat(81), LocalDate.of(2020, 10, 10));

			// When & Then
			mockMvc.perform(put(REQUEST_URL, coupleId, anniversaryId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_ANNIVERSARY_CONTENT));
		}

		@DisplayName("기념일 날짜가 yyyy-MM-dd 패턴이 아니라면 에러 코드, 메시지를 응답한다.")
		@NullAndEmptySource
		@CsvSource({"20201010", "2020/10/10", "2020.10.10"})
		@ParameterizedTest
		void withInvalidDatePattern(String date) throws Exception {

			// Given
			Long coupleId = 1L;
			Long anniversaryId = 1L;
			Map<String, String> request = createAnniversaryModifyRequestMap(date);

			// When & Then
			mockMvc.perform(put(REQUEST_URL, coupleId, anniversaryId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_DATE_PATTERN));
		}

		@DisplayName("기념일 날짜가 2049-12-31 이후라면 에러 코드, 메시지를 응답한다.")
		@Test
		void withInvalidDateRange() throws Exception {

			// Given
			Long coupleId = 1L;
			Long anniversaryId = 1L;
			AnniversaryModifyRequest request = createAnniversaryModifyRequest(
				"title", "content", LocalDate.of(2050, 1, 1));

			// When & Then
			mockMvc.perform(put(REQUEST_URL, coupleId, anniversaryId)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_CALENDER_TIME_RANGE));
		}
	}

	@Nested
	@DisplayName("기념일 삭제시")
	class DeleteAnniversary {

		private static final String REQUEST_URL = "/api/couples/{couple_id}/anniversary/{anniversary_id}";

		@BeforeEach
		void setUp() {
			Member member = createMember();
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("대상 기념일이 현재 로그인 회원의 커플 기념일이고, 생일, 처음만난날 관련 기념일이 아니면 요청에 성공한다.")
		@Test
		void withLoginMembersCouplesAnniversaryAndNotRelatedBirthDayAndFirstDate()
			throws Exception {

			// given
			Long coupleId = 1L;
			Long anniversaryId = 1L;

			// when & Then
			mockMvc.perform(delete(REQUEST_URL, coupleId, anniversaryId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}
	}

	private Member createMember() {

		return Member.builder()
			.name("홍길동")
			.nickname("nickname")
			.phone("01012341234")
			.password("password")
			.gender(Gender.MALE)
			.birthDay(LocalDate.of(1999, 10, 10))
			.build();
	}

	private Map<String, String> createAnniversaryCreateRequestMap(String repeatRule, String date) {

		HashMap<String, String> requestMap = new HashMap<>();

		requestMap.put("title", "title");
		requestMap.put("content", "content");
		requestMap.put("repeatRule", repeatRule);
		requestMap.put("date", date);

		return requestMap;
	}

	private AnniversaryDatesServiceResponse createAnniversaryDatesServiceResponse(
		List<LocalDate> dates) {

		return AnniversaryDatesServiceResponse.builder()
			.anniversaryDates(dates)
			.build();
	}

	private AnniversaryListServiceResponse createAnniversaryListServiceResponse() {

		AnniversaryServiceResponse serviceResponse = AnniversaryServiceResponse.builder()
			.id(1L)
			.title("title")
			.content("content")
			.repeatRule(AnniversaryRepeatRule.YEAR)
			.category(AnniversaryCategory.OTHER)
			.date(LocalDate.of(2021, 1, 1))
			.build();

		return AnniversaryListServiceResponse.builder()
			.anniversaries(List.of(serviceResponse))
			.build();
	}

	private ComingAnniversaryListServiceResponse createComingAnniversaryListServiceResponse() {

		ComingAnniversaryServiceResponse serviceResponse = ComingAnniversaryServiceResponse.builder()
			.id(1L)
			.title("title")
			.content("content")
			.date(LocalDate.of(2021, 1, 1))
			.build();

		return ComingAnniversaryListServiceResponse.builder()
			.anniversaries(List.of(serviceResponse))
			.build();
	}

	public AnniversaryModifyRequest createAnniversaryModifyRequest(String title, String content,
		LocalDate date) {

		return AnniversaryModifyRequest.builder()
			.title(title)
			.content(content)
			.date(date)
			.build();
	}

	private Map<String, String> createAnniversaryModifyRequestMap(String date) {

		HashMap<String, String> requestMap = new HashMap<>();

		requestMap.put("title", "title");
		requestMap.put("content", "content");
		requestMap.put("date", date);

		return requestMap;
	}
}
