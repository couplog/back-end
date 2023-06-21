package com.dateplan.dateplan.controller.schedule;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DATE_TIME_RANGE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DIFFERENCE_DATE_TIME;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_REPEAT_END_TIME_RANGE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_REPEAT_RULE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_LOCATION;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_TITLE;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_INPUT_VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleRequest;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceRequest;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

public class ScheduleControllerTest extends ControllerTestSupport {

	@Nested
	@DisplayName("개인 일정을 생성할 때")
	class CreateSchedule {

		private final static String REQUEST_URL = "/api/members/{member_id}/schedules";

		@DisplayName("올바른 멤버 id와, 일정 정보를 입력하면 성공한다.")
		@Test
		void successWithValidRequest() throws Exception {

			// Given
			ScheduleRequest request = createScheduleRequest();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("로그인한 회원에 대한 요청이 아니면 실패한다")
		@Test
		void failWithNoPermissionRequest() throws Exception {

			// Given
			ScheduleRequest request = createScheduleRequest();

			// Stub
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.READ);
			willThrow(expectedException)
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(expectedException.getErrorCode().getCode()))
				.andExpect(jsonPath("$.message").value(expectedException.getMessage()));
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
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

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

		@DisplayName("날짜 간격이 유효하지 않으면 실패한다")
		@CsvSource({"2023-01-01T15:00, 2023-01-02T16:00, D",
			"2023-01-01T15:00, 2023-01-09T16:00, W",
			"2023-01-01T15:00, 2023-02-02T16:00, M",
			"2023-01-01T15:00, 2024-01-02T16:00, Y",})
		@ParameterizedTest
		void failWithInvalidDifferenceDateTime(LocalDateTime startDateTime,
			LocalDateTime endDateTime,
			RepeatRule repeatRule) throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(startDateTime)
				.endDateTime(endDateTime)
				.repeatRule(repeatRule)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(
					jsonPath("$.code").value(ErrorCode.INVALID_DIFFERENCE_DATE_TIME.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_DIFFERENCE_DATE_TIME));
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
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

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
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

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
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

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
				.location(createLengthString(101))
				.content("content")
				.repeatRule(RepeatRule.M)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

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
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

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

}
