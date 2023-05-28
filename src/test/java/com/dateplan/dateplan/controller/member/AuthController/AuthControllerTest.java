package com.dateplan.dateplan.controller.member.AuthController;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_INPUT_VALUE;
import static com.dateplan.dateplan.global.exception.ErrorCode.MEMBER_NOT_FOUND;
import static com.dateplan.dateplan.global.exception.ErrorCode.PASSWORD_MISMATCH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.member.dto.LoginRequest;
import com.dateplan.dateplan.domain.member.dto.LoginServiceRequest;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.global.exception.auth.PasswordMismatchException;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.http.MediaType;

public class AuthControllerTest extends ControllerTestSupport {

	@Nested
	@DisplayName("로그인 시")
	class login {

		private static final String REQUEST_URL = "/api/auth/login";

		@DisplayName("올바른 전화번호와 패스워드를 입력하면 성공한다.")
		@Test
		void loginWithValidInput() throws Exception {

			// Given
			String phone = "01057840360";
			String password = "abcd1234";

			LoginRequest request = createLoginRequest(phone, password);

			willDoNothing().given(authService)
				.login(any(LoginServiceRequest.class), any(HttpServletResponse.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("잘못된 전화번호 패턴을 입력하면 실패한다.")
		@NullAndEmptySource
		@CsvSource({"010-1234-5678", "0101234567", "01012345678a", "A", "010a2345678"})
		@ParameterizedTest
		void loginWithInvalidInput(String phone) throws Exception {

			// Given
			String password = "abcd1234";

			LoginRequest request = createLoginRequest(phone, password);

			willDoNothing().given(authService)
				.login(any(LoginServiceRequest.class), any(HttpServletResponse.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.INVALID_PHONE_PATTERN));
		}

		@DisplayName("가입되지 않은 전화번호이면 실패한다.")
		@Test
		void loginWithNotRegisteredPhone() throws Exception {

			// Given
			String phone = "01012345678";
			String password = "abcd1234";

			LoginRequest request = createLoginRequest(phone, password);

			willThrow(new MemberNotFoundException()).given(authService)
				.login(any(LoginServiceRequest.class), any(HttpServletResponse.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(MEMBER_NOT_FOUND.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.MEMBER_NOT_FOUND));
		}

		@DisplayName("올바르지 않은 패스워드를 입력하면 실패한다")
		@Test
		void loginWithInvalidPassword() throws Exception {

			// Given
			String phone = "01012345678";
			String password = "abcd1234";

			LoginRequest request = createLoginRequest(phone, password);

			willThrow(new PasswordMismatchException()).given(authService)
				.login(any(LoginServiceRequest.class), any(HttpServletResponse.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(PASSWORD_MISMATCH.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.PASSWORD_MISMATCH));
		}
	}

	private LoginRequest createLoginRequest(String phone, String password) {
		return LoginRequest.builder()
			.phone(phone)
			.password(password)
			.build();
	}

}
