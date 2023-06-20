package com.dateplan.dateplan.controller.member.AuthController;

import static com.dateplan.dateplan.global.exception.ErrorCode.ALREADY_REGISTERED_NICKNAME;
import static com.dateplan.dateplan.global.exception.ErrorCode.ALREADY_REGISTERED_PHONE;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_INPUT_VALUE;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_PHONE_AUTH_CODE;
import static com.dateplan.dateplan.global.exception.ErrorCode.MEMBER_NOT_FOUND;
import static com.dateplan.dateplan.global.exception.ErrorCode.NOT_AUTHENTICATED_PHONE;
import static com.dateplan.dateplan.global.exception.ErrorCode.PASSWORD_MISMATCH;
import static com.dateplan.dateplan.global.exception.ErrorCode.PHONE_AUTH_LIMIT_OVER;
import static com.dateplan.dateplan.global.exception.ErrorCode.SMS_SEND_FAIL;
import static com.dateplan.dateplan.global.exception.ErrorCode.TOKEN_INVALID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.member.dto.AuthToken;
import com.dateplan.dateplan.domain.member.dto.login.LoginRequest;
import com.dateplan.dateplan.domain.member.dto.login.LoginServiceRequest;
import com.dateplan.dateplan.domain.member.dto.login.LoginServiceResponse;
import com.dateplan.dateplan.domain.member.dto.signup.PhoneAuthCodeRequest;
import com.dateplan.dateplan.domain.member.dto.signup.PhoneAuthCodeServiceRequest;
import com.dateplan.dateplan.domain.member.dto.signup.PhoneRequest;
import com.dateplan.dateplan.domain.member.dto.signup.PhoneServiceRequest;
import com.dateplan.dateplan.domain.member.dto.signup.SendSmsServiceResponse;
import com.dateplan.dateplan.domain.member.dto.signup.SignUpRequest;
import com.dateplan.dateplan.domain.member.dto.signup.SignUpServiceRequest;
import com.dateplan.dateplan.domain.sms.type.SmsType;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.auth.InvalidPhoneAuthCodeException;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.global.exception.auth.PasswordMismatchException;
import com.dateplan.dateplan.global.exception.auth.PhoneAuthLimitOverException;
import com.dateplan.dateplan.global.exception.auth.PhoneNotAuthenticatedException;
import com.dateplan.dateplan.global.exception.auth.TokenInvalidException;
import com.dateplan.dateplan.global.exception.member.AlReadyRegisteredNicknameException;
import com.dateplan.dateplan.global.exception.member.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.global.exception.sms.SmsSendFailException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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

			// Given
			String phoneNumber = "01012341234";
			PhoneRequest request = createPhoneRequest(phoneNumber);

			// Stub
			SendSmsServiceResponse serviceResponse = createSendSmsServiceResponse();
			given(authService.sendSms(any(PhoneServiceRequest.class)))
				.willReturn(serviceResponse);

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(jsonPath("$.data.currentCount").value(serviceResponse.getCurrentCount()));
		}

		@DisplayName("잘못된 패턴의 전화번호를 입력하면 요청에 실패한다.")
		@CsvSource({"010-1234-1234", "0101234123", "ㅁㅁ", "aa"})
		@NullAndEmptySource
		@ParameterizedTest
		void sendCodeWithInvalidPatternOfPhoneNumber(String phoneNumber) throws Exception {

			// Given
			PhoneRequest request = createPhoneRequest(phoneNumber);

			// Stub
			SendSmsServiceResponse serviceResponse = createSendSmsServiceResponse();
			given(authService.sendSms(any(PhoneServiceRequest.class)))
				.willReturn(serviceResponse);

			//When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_PHONE_PATTERN)
				);
		}

		@DisplayName("이미 가입된 전화번호를 입력하면 요청에 실패한다.")
		@Test
		void sendCodeWithRegisteredPhoneNumber() throws Exception {

			// Given
			String phoneNumber = "01012341234";
			PhoneRequest request = createPhoneRequest(phoneNumber);

			// Stub
			willThrow(new AlReadyRegisteredPhoneException())
				.given(authService)
				.sendSms(any(PhoneServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ALREADY_REGISTERED_PHONE.getCode()),
					jsonPath("$.message").value(DetailMessage.ALREADY_REGISTERED_PHONE)
				);
		}

		@DisplayName("외부 sms 서비스 에 문제가 있다면 요청에 실패한다.")
		@Test
		void sendCodeWithSmsServiceError() throws Exception {

			// Given
			String phoneNumber = "01012341234";
			PhoneRequest request = createPhoneRequest(phoneNumber);

			// Stub
			SmsSendFailException smsSendFailException =
				new SmsSendFailException(SmsType.PHONE_AUTHENTICATION);
			willThrow(smsSendFailException)
				.given(authService)
				.sendSms(any(PhoneServiceRequest.class));

			//When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isServiceUnavailable())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(SMS_SEND_FAIL.getCode()),
					jsonPath("$.message").value(smsSendFailException.getMessage())
				);
		}

		@DisplayName("전화번호 인증 요청 횟수가 초과했다면 요청에 실패한다.")
		@Test
		void sendCodeWithOverRequestLimitCount() throws Exception {

			// Given
			String phoneNumber = "01012341234";
			PhoneRequest request = createPhoneRequest(phoneNumber);

			// Stub
			willThrow(new PhoneAuthLimitOverException())
				.given(authService)
				.sendSms(any(PhoneServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isTooManyRequests())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(PHONE_AUTH_LIMIT_OVER.getCode()),
					jsonPath("$.message").value(DetailMessage.PHONE_AUTH_LIMIT_OVER)
				);
		}
	}

	@Nested
	@DisplayName("코드 인증시")
	class authenticateCode {

		private static final String REQUEST_URL = "/api/auth/phone/code";

		@DisplayName("올바른 전화번호, 인증 코드를 입력하면 요청에 성공한다.")
		@Test
		void authenticateCodeWithValidPhoneNumberAndCode() throws Exception {

			// Given
			String phoneNumber = "01012341234";
			String code = "123123";
			PhoneAuthCodeRequest request = createPhoneAuthCodeRequest(phoneNumber, code);

			// Stub
			willDoNothing()
				.given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
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

			//Stub
			willDoNothing()
				.given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			//When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_PHONE_PATTERN)
				);
		}

		@DisplayName("잘못된 패턴의 코드를 입력하면 요청에 실패한다.")
		@CsvSource({"12312", "asd", "ㅁㄴㅇ", "123-"})
		@NullAndEmptySource
		@ParameterizedTest
		void authenticateCodeWithInvalidPatternOfCode(String code) throws Exception {

			//Given
			String phoneNumber = "01012341234";
			PhoneAuthCodeRequest request = createPhoneAuthCodeRequest(phoneNumber, code);
			willDoNothing()
				.given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			//When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(DetailMessage.INVALID_PHONE_AUTH_CODE_PATTERN)
				);
		}

		@DisplayName("인증 시간이 지난 코드를 입력하면 요청에 실패한다.")
		@Test
		void authenticateCodeWithTimeOutCode() throws Exception {

			//Given
			String phoneNumber = "01012341234";
			String code = "123123";
			PhoneAuthCodeRequest request = createPhoneAuthCodeRequest(phoneNumber, code);

			//Stub
			InvalidPhoneAuthCodeException invalidPhoneAuthCodeException = new InvalidPhoneAuthCodeException(
				null);
			willThrow(invalidPhoneAuthCodeException)
				.given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			//When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_PHONE_AUTH_CODE.getCode()),
					jsonPath("$.message").value(invalidPhoneAuthCodeException.getMessage())
				);
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
			willThrow(invalidPhoneAuthCodeException)
				.given(authService)
				.authenticateAuthCode(any(PhoneAuthCodeServiceRequest.class));

			//When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_PHONE_AUTH_CODE.getCode()),
					jsonPath("$.message").value(invalidPhoneAuthCodeException.getMessage())
				);
		}
	}

	@Nested
	@DisplayName("회원가입 시")
	class SignUp {

		private static final String REQUEST_URL = "/api/auth/signup";

		@DisplayName("가입되지 않은 전화번호와 닉네임, 인증된 전화번호, 나머지 유효한 값을 입력하면 회원가입에 성공한다.")
		@Test
		void signUpWithValidInput() throws Exception {

			// Given
			String name = "이름";
			String nickname = "nickname";
			String phone = "01011112222";
			String password = "password";
			SignUpRequest request = createSignUpRequest(name, phone, nickname, password);

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isCreated());
		}

		@DisplayName("잘못된 패턴의 전화번호를 입력하면 회원가입에 실패한다.")
		@CsvSource({"010-1234-1234", "0101234123", "ㅁㅁ", "aa"})
		@NullAndEmptySource
		@ParameterizedTest
		void signUpWithInvalidPatternOfPhoneNumber(String phoneNumber) throws Exception {

			// Given
			String name = "이름";
			String nickname = "nickname";
			String password = "password";
			SignUpRequest request = createSignUpRequest(name, phoneNumber, nickname, password);

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
					jsonPath("$.message").value(DetailMessage.INVALID_PHONE_PATTERN)
				);
		}

		@DisplayName("잘못된 패턴의 이름을 입력하면 회원가입에 실패한다.")
		@CsvSource({"김", "김김김김김김김김김김김", "kim", "0101234", "김as", "af-김"})
		@NullAndEmptySource
		@ParameterizedTest
		void signUpWithInvalidPatternOfName(String name) throws Exception {

			// Given
			String phone = "01011112222";
			String nickname = "nickname";
			String password = "password";
			SignUpRequest request = createSignUpRequest(name, phone, nickname, password);

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
					jsonPath("$.message").value(DetailMessage.INVALID_MEMBER_NAME_PATTERN)
				);
		}

		@DisplayName("잘못된 패턴의 닉네임을 입력하면 회원가입에 실패한다.")
		@CsvSource({"a", "aaaaaaaaaaa", "as!"})
		@NullAndEmptySource
		@ParameterizedTest
		void signUpWithInvalidPatternOfNickname(String nickname) throws Exception {

			// Given
			String phone = "01011112222";
			String name = "이름";
			String password = "password";
			SignUpRequest request = createSignUpRequest(name, phone, nickname, password);

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
					jsonPath("$.message").value(DetailMessage.INVALID_NICKNAME_PATTERN)
				);
		}

		@DisplayName("잘못된 패턴의 비밀번호를 입력하면 회원가입에 실패한다.")
		@CsvSource({"aaaa", "aaaaaaaaaaaaaaaaaaaaa", "aaaaa!", "aaaaa김"})
		@NullAndEmptySource
		@ParameterizedTest
		void signUpWithInvalidPatternOfPassword(String password) throws Exception {

			// Given
			String phone = "01011112222";
			String name = "이름";
			String nickname = "nickname";
			SignUpRequest request = createSignUpRequest(name, phone, nickname, password);

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
					jsonPath("$.message").value(DetailMessage.INVALID_PASSWORD_PATTERN)
				);
		}

		@DisplayName("잘못된 패턴의 생년월일을 입력하면 회원가입에 실패한다.")
		@CsvSource({"2020/12/12", "2020.12.12", "20201212", "2020-13-11", "2020-12-32"})
		@NullAndEmptySource
		@ParameterizedTest
		void signUpWithInvalidPatternOfBirth(String birth) throws Exception {

			// Given
			String gender = "male";
			Map<String, String> request = createSignUpRequestWithMap(birth, gender);

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
					jsonPath("$.message").value(DetailMessage.INVALID_DATE_PATTERN)
				);
		}

		@DisplayName("미래의 날짜로 생년월일을 입력하면 회원가입에 실패한다.")
		@Test
		void signUpWithInvalidRangeOfBirth() throws Exception {

			// Given
			String gender = "male";
			String birth = "2099-10-10";
			Map<String, String> request = createSignUpRequestWithMap(birth, gender);

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
					jsonPath("$.message").value(DetailMessage.INVALID_BIRTH_RANGE)
				);
		}

		@DisplayName("잘못된 패턴의 성별을 입력하면 회원가입에 실패한다.")
		@CsvSource({"남자", "여자", "남", "여"})
		@NullAndEmptySource
		@ParameterizedTest
		void signUpWithInvalidPatternOfGender(String gender) throws Exception {

			// Given
			String birth = "1999-01-01";
			Map<String, String> request = createSignUpRequestWithMap(birth, gender);

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
					jsonPath("$.message").value(DetailMessage.INVALID_GENDER)
				);
		}

		@DisplayName("이미 가입된 전화번호를 입력하면 회원가입에 실패한다.")
		@Test
		void signUpWithAlreadyRegisteredPhone() throws Exception {

			// Given
			String name = "이름";
			String nickname = "nickname";
			String phone = "01011112222";
			String password = "password";
			SignUpRequest request = createSignUpRequest(name, phone, nickname, password);

			// Stub
			AlReadyRegisteredPhoneException expectedException = new AlReadyRegisteredPhoneException();
			willThrow(expectedException)
				.given(memberService)
				.signUp(any(SignUpServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ALREADY_REGISTERED_PHONE.getCode()),
					jsonPath("$.message").value(DetailMessage.ALREADY_REGISTERED_PHONE)
				);
		}

		@DisplayName("이미 등록된 닉네임을 입력하면 회원가입에 실패한다.")
		@Test
		void signUpWithAlreadyRegisteredNickname() throws Exception {

			// Given
			String name = "이름";
			String nickname = "nickname";
			String phone = "01011112222";
			String password = "password";
			SignUpRequest request = createSignUpRequest(name, phone, nickname, password);

			// Stub
			AlReadyRegisteredNicknameException expectedException = new AlReadyRegisteredNicknameException();
			willThrow(expectedException)
				.given(memberService)
				.signUp(any(SignUpServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ALREADY_REGISTERED_NICKNAME.getCode()),
					jsonPath("$.message").value(DetailMessage.ALREADY_REGISTERED_NICKNAME)
				);
		}

		@DisplayName("전화번호 인증을 성공하지 않은 전화번호를 입력하면 회원가입에 실패한다.")
		@Test
		void signUpWithNotAuthenticatedPhone() throws Exception {

			// Given
			String name = "이름";
			String nickname = "nickname";
			String phone = "01011112222";
			String password = "password";
			SignUpRequest request = createSignUpRequest(name, phone, nickname, password);

			// Stub
			PhoneNotAuthenticatedException expectedException = new PhoneNotAuthenticatedException();
			willThrow(expectedException)
				.given(memberService)
				.signUp(any(SignUpServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(NOT_AUTHENTICATED_PHONE.getCode()),
					jsonPath("$.message").value(DetailMessage.NOT_AUTHENTICATED_PHONE)
				);
		}
	}

	@Nested
	@DisplayName("로그인 시")
	class Login {

		private static final String REQUEST_URL = "/api/auth/login";

		@DisplayName("올바른 전화번호와 패스워드를 입력하면 성공하고, 연결 여부를 리턴한다.")
		@Test
		void loginWithValidInput() throws Exception {

			// Given
			String phone = "01057840360";
			String password = "abcd1234";
			Boolean isConnected = true;

			LoginRequest request = createLoginRequest(phone, password);
			AuthToken authToken = createAuthToken();
			LoginServiceResponse response = LoginServiceResponse.builder()
				.authToken(authToken)
				.isConnected(isConnected)
				.build();

			// Stub
			willReturn(response)
				.given(authService)
				.login(any(LoginServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(jsonPath("$.data.isConnected").value(isConnected))
				.andExpect(header().string("Authorization", authToken.getAccessToken()))
				.andExpect(header().string("refreshToken", authToken.getRefreshToken()));
		}

		@DisplayName("가입되지 않은 전화번호이면 실패한다.")
		@Test
		void loginWithNotRegisteredPhone() throws Exception {

			// Given
			String phone = "01012345678";
			String password = "abcd1234";

			LoginRequest request = createLoginRequest(phone, password);

			// Stub
			willThrow(new MemberNotFoundException())
				.given(authService)
				.login(any(LoginServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
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

			// Stub
			willThrow(new PasswordMismatchException())
				.given(authService)
				.login(any(LoginServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(PASSWORD_MISMATCH.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.PASSWORD_MISMATCH))
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("refreshToken"));
		}

		@DisplayName("전화번호를 입력하지 않으면 실패한다")
		@Test
		void failWithPhoneIsNull() throws Exception {

			// Given
			LoginRequest request = LoginRequest.builder()
				.password("password")
				.build();

			// Stub
			given(authService.login(any(LoginServiceRequest.class)))
				.willThrow(new MemberNotFoundException());

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(MEMBER_NOT_FOUND.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.MEMBER_NOT_FOUND))
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("refreshToken"));
		}

		@DisplayName("비밀번호를 입력하지 않으면 실패한다")
		@Test
		void failWithPasswordIsNull() throws Exception {

			// Given
			LoginRequest request = LoginRequest.builder()
				.phone("01011112222")
				.build();

			// Stub
			given(authService.login(any(LoginServiceRequest.class)))
				.willThrow(new PasswordMismatchException());

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL)
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
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

			// Stub
			willReturn(authToken)
				.given(authService)
				.refreshAccessToken(any(String.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL).header("Authorization", preToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(header().string("Authorization", authToken.getAccessToken()))
				.andExpect(header().string("refreshToken", authToken.getRefreshToken()));
		}

		@DisplayName("refresh token을 입력하지 않으면 실패한다.")
		@Test
		void refreshWithoutToken() throws Exception {

			// Stub
			willThrow(new TokenInvalidException()).given(authService)
				.refreshAccessToken(any(String.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL))
				.andExpect(status().isBadRequest())
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("rereshToken"));
		}

		@DisplayName("유효하지 않은 token을 입력하면 실패한다.")
		@Test
		void refreshWithInvalidToken() throws Exception {

			// Stub
			willThrow(new TokenInvalidException())
				.given(authService)
				.refreshAccessToken(any(String.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL).header("Authorization", "token"))
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

			// Stub
			willThrow(new MemberNotFoundException())
				.given(authService)
				.refreshAccessToken(any(String.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL).header("Authorization", "token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(MEMBER_NOT_FOUND.getCode()))
				.andExpect(jsonPath("$.message").value(DetailMessage.MEMBER_NOT_FOUND))
				.andExpect(header().doesNotExist("Authorization"))
				.andExpect(header().doesNotExist("refreshToken"));
		}
	}

	private SignUpRequest createSignUpRequest(String name, String phoneNumber, String nickname,
		String password) {

		return SignUpRequest.builder()
			.name(name)
			.nickname(nickname)
			.phone(phoneNumber)
			.birthDay(LocalDate.of(1999, 10, 10))
			.password(password)
			.gender(Gender.MALE).build();
	}

	private Map<String, String> createSignUpRequestWithMap(String birthDay, String gender) {

		Map<String, String> request = new HashMap<>();
		request.put("name", "이름");
		request.put("nickname", "nickname");
		request.put("phone", "01011112222");
		request.put("birthDay", birthDay);
		request.put("password", "password");
		request.put("gender", gender);

		return request;
	}

	private LoginRequest createLoginRequest(String phone, String password) {
		return LoginRequest.builder().phone(phone).password(password).build();
	}

	private AuthToken createAuthToken() {
		return AuthToken.builder().accessToken("accessToken").refreshToken("refreshToken").build();
	}

	private PhoneRequest createPhoneRequest(String phone) {

		return new PhoneRequest(phone);
	}

	private PhoneAuthCodeRequest createPhoneAuthCodeRequest(String phone, String code) {

		return new PhoneAuthCodeRequest(phone, code);
	}

	private SendSmsServiceResponse createSendSmsServiceResponse() {

		return SendSmsServiceResponse.builder()
			.currentCount(1)
			.build();
	}
}
