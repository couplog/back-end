package com.dateplan.dateplan.controller.member;

import static com.dateplan.dateplan.global.exception.ErrorCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.member.dto.PhoneAuthCodeRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneAuthCodeServiceRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneServiceRequest;
import com.dateplan.dateplan.domain.sms.type.SmsType;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.InvalidPhoneAuthCodeException;
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

	private PhoneRequest createPhoneRequest(String phone) {

		return new PhoneRequest(phone);
	}

	private PhoneAuthCodeRequest createPhoneAuthCodeRequest(String phone, String code) {

		return new PhoneAuthCodeRequest(phone, code);
	}
}
