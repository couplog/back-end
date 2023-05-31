package com.dateplan.dateplan.controller.member.AuthController;

import static com.dateplan.dateplan.global.exception.ErrorCode.ALREADY_REGISTERED_PHONE;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_INPUT_VALUE;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_PHONE_AUTH_CODE;
import static com.dateplan.dateplan.global.exception.ErrorCode.MEMBER_NOT_FOUND;
import static com.dateplan.dateplan.global.exception.ErrorCode.PASSWORD_MISMATCH;
import static com.dateplan.dateplan.global.exception.ErrorCode.SMS_SEND_FAIL;
import static com.dateplan.dateplan.global.exception.ErrorCode.TOKEN_INVALID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.member.dto.AuthToken;
import com.dateplan.dateplan.domain.member.dto.LoginRequest;
import com.dateplan.dateplan.domain.member.dto.LoginServiceRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneAuthCodeRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneAuthCodeServiceRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneServiceRequest;
import com.dateplan.dateplan.domain.sms.type.SmsType;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.InvalidPhoneAuthCodeException;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.global.exception.auth.PasswordMismatchException;
import com.dateplan.dateplan.global.exception.auth.TokenInvalidException;
import com.dateplan.dateplan.global.exception.sms.SmsSendFailException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.http.MediaType;

class AuthControllerTest extends ControllerTestSupport {

	@Nested
	@DisplayName("코드 전송시")
	class sendCode {

		private static final String REQUEST_URL = "/api/auth/phone";

		@DisplayName("유효한 전화번호를 입력하면 요청에 성공한다.")
		@Test
		void sendCodeWithValidPhoneNumber() throws Exception {

			//Given
			String phoneNumber = "01012341234";
			PhoneRequest request = createPhoneRequest(phoneNumber);
			willDoNothing().given(authService).sendSms(any(PhoneServiceRequest.class));

			//When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("잘못된 패턴의 전화번호를 입력하면 요청에 실패한다.")
		@CsvSource({"010-1234-1234", "0101234123", "ㅁㅁ", "aa"})
		@NullAndEmptySource
		@ParameterizedTest
		void sendCodeWithInvalidPatternOfPhoneNumber(String phoneNumber) throws Exception {

			//Given
			PhoneRequest request = createPhoneRequest(phoneNumber);
			willDoNothing().given(authService).sendSms(any(PhoneServiceRequest.class));

			//When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_PHONE_PATTERN));
		}

		@DisplayName("이미 가입된 전화번호를 입력하면 요청에 실패한다.")
		@Test
		void sendCodeWithRegisteredPhoneNumber() throws Exception {

			//Given
			String phoneNumber = "01012341234";
			PhoneRequest request = createPhoneRequest(phoneNumber);
			willThrow(new AlReadyRegisteredPhoneException()).given(authService)
				.sendSms(any(PhoneServiceRequest.class));

			//When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ALREADY_REGISTERED_PHONE.getCode()),
					jsonPath("$.message").value(DetailMessage.ALREADY_REGISTERED_PHONE));
		}

		@DisplayName("외부 sms 서비스 에 문제가 있다면 요청에 실패한다.")
		@Test
		void sendCodeWithSmsServiceError() throws Exception {

			//Given
			String phoneNumber = "01012341234";
			PhoneRequest request = createPhoneRequest(phoneNumber);
			SmsSendFailException smsSendFailException = new SmsSendFailException(
				SmsType.PHONE_AUTHENTICATION);
			willThrow(smsSendFailException).given(authService)
				.sendSms(any(PhoneServiceRequest.class));

			//When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isServiceUnavailable())
				.andExpectAll(jsonPath("$.success").value("false"),
					jsonPath("$.code").value(SMS_SEND_FAIL.getCode()),
					jsonPath("$.message").value(smsSendFailException.getMessage()));
		}
	}

	@Nested
	@DisplayName("코드 인증시")
	class authenticateCode {

		private static final String REQUEST_URL = "/api/auth/phone/code";

		@DisplayName("올바른 전화번호, 인증 코드를 입력하면 요청에 성공한다.")
		@Test
		void authenticateCodeWithValidPhoneNumberAndCode() throws Exception {

			//Given
			String phoneNumber = "01012341234";
			String code = "123123";
			PhoneAuthCodeRequest request = createPhoneAuthCodeRequest(phoneNumber, code);
			willDoNothing().given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			//When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("잘못된 패턴의 전화번호를 입력하면 요청에 실패한다.")
		@CsvSource({"010-1234-1234", "0101234123", "ㅁㅁ", "aa"})
		@NullAndEmptySource
		@ParameterizedTest
		void authenticateCodeWithInvalidPatternOfPhoneNumber(String phoneNumber) throws Exception {

			//Given
			String code = "123123";
			PhoneAuthCodeRequest request = createPhoneAuthCodeRequest(phoneNumber, code);
			willDoNothing().given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			//When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_PHONE_PATTERN));

		}

		@DisplayName("잘못된 패턴의 코드를 입력하면 요청에 실패한다.")
		@CsvSource({"12312", "asd", "ㅁㄴㅇ", "123-"})
		@NullAndEmptySource
		@ParameterizedTest
		void authenticateCodeWithInvalidPatternOfCode(String code) throws Exception {

			//Given
			String phoneNumber = "01012341234";
			PhoneAuthCodeRequest request = createPhoneAuthCodeRequest(phoneNumber, code);
			willDoNothing().given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			//When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_PHONE_AUTH_CODE_PATTERN));
		}

		@DisplayName("인증 시간이 지난 코드를 입력하면 요청에 실패한다.")
		@Test
		void authenticateCodeWithTimeOutCode() throws Exception {

			//Given
			String phoneNumber = "01012341234";
			String code = "123123";
			PhoneAuthCodeRequest request = createPhoneAuthCodeRequest(phoneNumber, code);
			InvalidPhoneAuthCodeException invalidPhoneAuthCodeException = new InvalidPhoneAuthCodeException(
				null);
			willThrow(invalidPhoneAuthCodeException).given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			//When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_PHONE_AUTH_CODE.getCode()),
					jsonPath("$.message").value(invalidPhoneAuthCodeException.getMessage()));
		}

		@DisplayName("일치하지 않는 코드를 입력하면 요청에 실패한다.")
		@Test
		void authenticateCodeWithNotMatchedCode() throws Exception {

			//Given
			String phoneNumber = "01012341234";
			String code = "123123";
			PhoneAuthCodeRequest request = createPhoneAuthCodeRequest(phoneNumber, code);
			InvalidPhoneAuthCodeException invalidPhoneAuthCodeException = new InvalidPhoneAuthCodeException(
				code);
			willThrow(invalidPhoneAuthCodeException).given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			//When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_PHONE_AUTH_CODE.getCode()),
					jsonPath("$.message").value(invalidPhoneAuthCodeException.getMessage()));
		}
	}

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
			AuthToken authToken = createAuthToken();

			willReturn(authToken).given(authService).login(any(LoginServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(header().string("Authorization", authToken.getAccessToken()))
				.andExpect(header().string("refreshToken", authToken.getRefreshToken()));
		}

		@DisplayName("잘못된 전화번호 패턴을 입력하면 실패한다.")
		@NullAndEmptySource
		@CsvSource({"010-1234-5678", "0101234567", "01012345678a", "A", "010a2345678"})
		@ParameterizedTest
		void loginWithInvalidInput(String phone) throws Exception {

			// Given
			String password = "abcd1234";

			LoginRequest request = createLoginRequest(phone, password);
			AuthToken authToken = createAuthToken();

			willReturn(authToken).given(authService).login(any(LoginServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.INVALID_PHONE_PATTERN))
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("refreshToken"));
		}

		@DisplayName("가입되지 않은 전화번호이면 실패한다.")
		@Test
		void loginWithNotRegisteredPhone() throws Exception {

			// Given
			String phone = "01012345678";
			String password = "abcd1234";

			LoginRequest request = createLoginRequest(phone, password);

			willThrow(new MemberNotFoundException()).given(authService)
				.login(any(LoginServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(MEMBER_NOT_FOUND.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.MEMBER_NOT_FOUND))
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("refreshToken"));
		}

		@DisplayName("올바르지 않은 패스워드를 입력하면 실패한다")
		@Test
		void loginWithInvalidPassword() throws Exception {

			// Given
			String phone = "01012345678";
			String password = "abcd1234";

			LoginRequest request = createLoginRequest(phone, password);

			willThrow(new PasswordMismatchException()).given(authService)
				.login(any(LoginServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(PASSWORD_MISMATCH.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.PASSWORD_MISMATCH))
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("refreshToken"));
		}
	}

	@Nested
	@DisplayName("토큰 재발급 시")
	class refresh {

		private static final String REQUEST_URL = "/api/auth/refresh";

		@DisplayName("올바른 refresh token을 입력하면 성공한다.")
		@Test
		void refreshWithValidToken() throws Exception {

			// Given
			String preToken = "preToken";
			AuthToken authToken = createAuthToken();

			willReturn(authToken).given(authService).refreshAccessToken(any(String.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).header("Authorization", preToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(header().string("Authorization", authToken.getAccessToken()))
				.andExpect(header().string("refreshToken", authToken.getRefreshToken()));
		}

		@DisplayName("refresh token을 입력하지 않으면 실패한다.")
		@Test
		void refreshWithoutToken() throws Exception {

			// Given
			willThrow(new TokenInvalidException()).given(authService)
				.refreshAccessToken(any(String.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL))
				.andExpect(status().isBadRequest())
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("rereshToken"));
		}

		@DisplayName("유효하지 않은 token을 입력하면 실패한다.")
		@Test
		void refreshWithInvalidToken() throws Exception {

			// Given
			willThrow(new TokenInvalidException()).given(authService)
				.refreshAccessToken(any(String.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).header("Authorization", "token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(TOKEN_INVALID.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.TOKEN_INVALID))
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("refreshToken"));
		}

		@DisplayName("토큰으로 회원을 찾을 수 없으면 실패한다.")
		@Test
		void refreshWithMemberNotFound() throws Exception {

			// Given
			willThrow(new MemberNotFoundException()).given(authService)
				.refreshAccessToken(any(String.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL).header("Authorization", "token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(MEMBER_NOT_FOUND.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.MEMBER_NOT_FOUND))
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("refreshToken"));
		}

	}

	private LoginRequest createLoginRequest(String phone, String password) {
		return LoginRequest.builder()
			.phone(phone)
			.password(password)
			.build();
	}

	private AuthToken createAuthToken() {
		return AuthToken.builder()
			.accessToken("accessToken")
			.refreshToken("refreshToken")
			.build();
	}

	private PhoneRequest createPhoneRequest(String phone) {

		return new PhoneRequest(phone);
	}

	private PhoneAuthCodeRequest createPhoneAuthCodeRequest(String phone, String code) {

		return new PhoneAuthCodeRequest(phone, code);
	}
}
