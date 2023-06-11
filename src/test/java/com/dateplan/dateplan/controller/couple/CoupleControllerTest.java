package com.dateplan.dateplan.controller.couple;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.couple.dto.CoupleInfoServiceResponse;
import com.dateplan.dateplan.domain.couple.dto.FirstDateRequest;
import com.dateplan.dateplan.domain.couple.dto.FirstDateServiceRequest;
import com.dateplan.dateplan.domain.couple.dto.FirstDateServiceResponse;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public class CoupleControllerTest extends ControllerTestSupport {

	@BeforeEach
	void setUp() throws Exception {
		given(
			authInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class),
				any(Object.class)))
			.willReturn(true);
	}

	@Nested
	@DisplayName("커플 처음 만난 날 조회 시")
	class GetFirstDate {

		private static final String REQUEST_URL = "/api/couples/{couple_id}/first-date";

		@DisplayName("현재 자신이 커플에 연결돼 있는 상태이고, 올바른 커플 id를 요청하면 성공한다.")
		@Test
		void successWithValidCoupleId() throws Exception {

			// Given
			FirstDateServiceResponse response = createFirstDateResponse();

			// Stub
			given(coupleService.getFirstDate(anyLong()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL, 1L))
				.andExpect(status().isOk())
				.andExpectAll(
					jsonPath("$.success").value("true"),
					jsonPath("$.data.firstDate").value(response.getFirstDate().toString()));
		}

		@DisplayName("Long 타입이 아닌 couple-id를 적으면 실패한다.")
		@Test
		void failWithInvalidType() throws Exception {

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL, "a"))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.getCode()));
		}

		@DisplayName("현재 자신이 커플에 연결되어 있지 않으면 실패한다")
		@Test
		void failWithNotConnected() throws Exception {

			// Stub
			given(coupleService.getFirstDate(anyLong()))
				.willThrow(new MemberNotConnectedException());

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL, 1L))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.MEMBER_NOT_CONNECTED.getCode()),
					jsonPath("$.message").value(DetailMessage.Member_NOT_CONNECTED));
		}

		@DisplayName("현재 자신이 연결되어 있는 커플의 id와 path variable의 couple-id가 다르면 실패한다")
		@Test
		void failWithNotMatchedCoupleId() throws Exception {

			// Given
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.READ);

			// Stub
			given(coupleService.getFirstDate(anyLong()))
				.willThrow(exception);

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL, 1L))
				.andExpect(status().isForbidden())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage()));
		}
	}

	@Nested
	@DisplayName("커플 처음 만난 날 수정 시")
	class UpdateFirstDate {

		private static final String REQUEST_URL = "/api/couples/{couple_id}/first-date";

		@DisplayName("올바른 coupleId, request를 요청하면 성공한다")
		@Test
		void successWithValidCoupleIdAndRequest() throws Exception {

			// Given
			FirstDateRequest request = createFirstDateRequest();

			// Stub
			willDoNothing()
				.given(coupleService)
				.updateFirstDate(anyLong(), any(FirstDateServiceRequest.class));

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1L)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("Long 타입이 아닌 couple-id를 적으면 실패한다.")
		@Test
		void failWithInvalidType() throws Exception {

			FirstDateRequest request = createFirstDateRequest();

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, "a")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.getCode()));
		}

		@DisplayName("현재 자신이 커플에 연결되어 있지 않으면 실패한다")
		@Test
		void failWithNotConnected() throws Exception {

			// Given
			FirstDateRequest request = createFirstDateRequest();

			// Stub
			willThrow(new MemberNotConnectedException())
				.given(coupleService)
				.updateFirstDate(anyLong(), any(FirstDateServiceRequest.class));

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1L)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.MEMBER_NOT_CONNECTED.getCode()),
					jsonPath("$.message").value(DetailMessage.Member_NOT_CONNECTED));
		}

		@DisplayName("현재보다 이후의 시간을 처음 만난 날로 수정하려하면 실패한다")
		@Test
		void failWithNowPastFirstDate() throws Exception {

			// Given
			FirstDateRequest request = FirstDateRequest.builder()
				.firstDate(LocalDate.now().plusDays(1L))
				.build();

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1L)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_FIRST_DATE_RANGE));
		}

		@DisplayName("현재 자신이 연결되어 있는 커플의 id와 path variable의 couple-id가 다르면 실패한다")
		@Test
		void failWithNotMatchedCoupleId() throws Exception {

			// Given
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.UPDATE);
			FirstDateRequest request = createFirstDateRequest();

			// Stub
			willThrow(exception)
				.given(coupleService)
				.updateFirstDate(anyLong(), any(FirstDateServiceRequest.class));

			// When & Then
			mockMvc.perform(
					put(REQUEST_URL, 1L)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isForbidden())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(exception.getErrorCode().getCode()),
					jsonPath("$.message").value(exception.getMessage()));
		}
	}

	@Nested
	@DisplayName("현재 연결되어 있는 커플 정보를 조회하려 할 때")
	class GetCoupleInfo {

		private static final String REQUEST_URL = "/api/couples/me";

		@DisplayName("현재 커플에 연결되어 있다면, 성공한다")
		@Test
		void successWithConnected() throws Exception {

			// Given
			CoupleInfoServiceResponse response = createCoupleInfoServiceResponse();

			// Stub
			given(coupleReadService.getCoupleInfo())
				.willReturn(response);

			// When & Then
			mockMvc.perform(
				get(REQUEST_URL))
				.andExpect(status().isOk())
				.andExpectAll(
					jsonPath("$.success").value("true"),
					jsonPath("$.data.coupleId").value(response.getCoupleId()),
					jsonPath("$.data.partnerId").value(response.getPartnerId()),
					jsonPath("$.data.firstDate").value(response.getFirstDate().toString())
				);
		}

		@DisplayName("현재 커플에 연결되어 있지 않다면 실패한다")
		@Test
		void failWithNotConnected() throws Exception {

			// Stub
			given(coupleReadService.getCoupleInfo())
				.willThrow(new MemberNotConnectedException());

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.MEMBER_NOT_CONNECTED.getCode()),
					jsonPath("$.message").value(DetailMessage.Member_NOT_CONNECTED)
				);
		}
	}

	private FirstDateServiceResponse createFirstDateResponse() {
		return FirstDateServiceResponse.builder()
			.firstDate(LocalDate.of(2010, 10, 10))
			.build();
	}

	private FirstDateRequest createFirstDateRequest() {
		return FirstDateRequest.builder()
			.firstDate(LocalDate.of(2010, 10, 10))
			.build();
	}

	private CoupleInfoServiceResponse createCoupleInfoServiceResponse() {
		return CoupleInfoServiceResponse.builder()
			.coupleId(1L)
			.partnerId(1L)
			.firstDate(LocalDate.of(2010, 10, 10))
			.build();
	}
}
