package com.dateplan.dateplan.controller.couple;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.couple.dto.FirstDateServiceResponse;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.NoPermissionException;
import com.dateplan.dateplan.global.exception.couple.CoupleNotConnectedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
				.willThrow(new CoupleNotConnectedException());

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL, 1L))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.COUPLE_NOT_CONNECTED.getCode()),
					jsonPath("$.message").value(DetailMessage.COUPLE_NOT_CONNECTED));
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

	private FirstDateServiceResponse createFirstDateResponse() {
		return FirstDateServiceResponse.builder()
			.firstDate(LocalDate.of(2010, 10, 10))
			.build();
	}
}
