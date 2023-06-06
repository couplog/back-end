package com.dateplan.dateplan.controller.member.MemberController;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_CONNECTED;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CONNECTION_CODE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CONNECTION_CODE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.SELF_CONNECTION_NOT_ALLOWED;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_INPUT_VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.member.dto.ConnectionRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.member.AlreadyConnectedException;
import com.dateplan.dateplan.global.exception.member.InvalidConnectionCodeException;
import com.dateplan.dateplan.global.exception.member.SelfConnectionNotAllowedException;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.http.MediaType;

public class MemberControllerTest extends ControllerTestSupport {

	@Nested
	@DisplayName("회원 연결 코드 조회 시")
	class GetConnectionCode {

		private static final String REQUEST_URL = "/api/members/connect";

		@BeforeEach
		void setUp() {
			willReturn(Optional.of("token"))
				.given(jwtProvider)
				.resolveToken(any(HttpServletRequest.class));
			willReturn(true)
				.given(jwtProvider)
				.isValid(anyString());
		}

		@DisplayName("인증되지 않은 유저는 실패한다")
		@Test
		void failWithNotAuthenticated() throws Exception {
			willReturn(Optional.empty())
				.given(jwtProvider)
				.resolveToken(any(HttpServletRequest.class));

			mockMvc.perform(
					get(REQUEST_URL))
				.andExpect(status().isUnauthorized())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.message").value(DetailMessage.TOKEN_NOT_FOUND),
					jsonPath("$.code").value(ErrorCode.TOKEN_NOT_FOUND.getCode())
				);
		}

		@DisplayName("생성한 코드 또는 24시간 내에 생성된 코드를 반환한다")
		@Test
		void returnConnectionCode() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionServiceResponse response = createConnectionServiceResponse(connectionCode);

			// Stub
			given(coupleService.getConnectionCode())
				.willReturn(response);

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL))
				.andExpect(status().isOk())
				.andExpectAll(
					jsonPath("$.success").value("true"),
					jsonPath("$.data.connectionCode").value(connectionCode)
				);
		}

	}

	@Nested
	@DisplayName("회원 연결 시")
	class ConnectCouple {

		private static final String REQUEST_URL = "/api/members/connect";

		@BeforeEach
		void setUp() {
			willReturn(Optional.of("token"))
				.given(jwtProvider)
				.resolveToken(any(HttpServletRequest.class));
			willReturn(true)
				.given(jwtProvider)
				.isValid(anyString());
		}

		@DisplayName("인증되지 않은 유저는 실패한다")
		@Test
		void failWithNotAuthenticated() throws Exception {
			willReturn(Optional.empty())
				.given(jwtProvider)
				.resolveToken(any(HttpServletRequest.class));

			mockMvc.perform(
					get(REQUEST_URL))
				.andExpect(status().isUnauthorized())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.message").value(DetailMessage.TOKEN_NOT_FOUND),
					jsonPath("$.code").value(ErrorCode.TOKEN_NOT_FOUND.getCode())
				);
		}

		@DisplayName("상대의 올바른 연결 코드를 입력하면 커플 생성에 성공한다.")
		@Test
		void successConnectCouple() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willDoNothing()
				.given(coupleService)
				.connectCouple(any(ConnectionServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("올바르지 않은 패턴의 연결 코드를 입력하면 실패한다")
		@CsvSource({"12345", "123abc", "abcde", "패턴"})
		@NullAndEmptySource
		@ParameterizedTest
		void connectCoupleWithInvalidConnectionCodePattern(String connectionCode) throws Exception {

			// Given
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willDoNothing()
				.given(coupleService)
				.connectCouple(any(ConnectionServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(INVALID_CONNECTION_CODE_PATTERN)
				);
		}

		@DisplayName("존재하지 않은 연결 코드를 입력하면 실패한다")
		@Test
		void connectCoupleWithNotFoundCode() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willThrow(new InvalidConnectionCodeException())
				.given(coupleService)
				.connectCouple(any(ConnectionServiceRequest.class));

			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.INVALID_CONNECTION_CODE.getCode()),
					jsonPath("$.message").value(INVALID_CONNECTION_CODE)
				);
		}

		@DisplayName("상대방이 이미 연결되어있다면 실패한다")
		@Test
		void connectCoupleWithAlreadyConnected() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willThrow(new AlreadyConnectedException())
				.given(coupleService)
				.connectCouple(any(ConnectionServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.ALREADY_CONNECTED.getCode()),
					jsonPath("$.message").value(ALREADY_CONNECTED)
				);
		}

		@DisplayName("자기 자신과 연결하려 하면 실패한다")
		@Test
		void connectCoupleWithSelf() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willThrow(new SelfConnectionNotAllowedException())
				.given(coupleService)
				.connectCouple(any(ConnectionServiceRequest.class));

			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.SELF_CONNECTION_NOT_ALLOWED.getCode()),
					jsonPath("$.message").value(SELF_CONNECTION_NOT_ALLOWED)
				);
		}

	}

	public ConnectionServiceResponse createConnectionServiceResponse(String connectionCode) {
		return ConnectionServiceResponse.builder()
			.connectionCode(connectionCode)
			.build();
	}

	public ConnectionRequest createConnectionRequest(String connectionCode) {
		return ConnectionRequest.builder()
			.connectionCode(connectionCode)
			.firstDate(LocalDate.now().minusDays(1L))
			.build();
	}

}
