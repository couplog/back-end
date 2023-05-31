package com.dateplan.dateplan.service.member;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_REGISTERED_PHONE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.PASSWORD_MISMATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.dateplan.dateplan.domain.member.dto.AuthToken;
import com.dateplan.dateplan.domain.member.dto.LoginServiceRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneAuthCodeServiceRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.domain.sms.type.SmsType;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.global.exception.InvalidPhoneAuthCodeException;
import com.dateplan.dateplan.global.exception.auth.PasswordMismatchException;
import com.dateplan.dateplan.global.exception.sms.SmsSendFailException;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
import com.dateplan.dateplan.service.ServiceTestSupport;
import org.jasypt.util.password.PasswordEncryptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class AuthServiceTest extends ServiceTestSupport {

	@Autowired
	private AuthService authService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncryptor passwordEncryptor;

	@SpyBean
	private StringRedisTemplate redisTemplate;

	@Nested
	@DisplayName("코드 전송시")
	class sendCode {

		@AfterEach
		void tearDown() {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
		}

		@DisplayName("유효한 전화번호를 입력하면 인증 코드가 발송되고, redis 에 저장된다.")
		@Test
		void sendCodeWithValidPhoneNumber() {

			// Given
			String phoneNumber = "01011112222";
			String authKey = "[AUTH]01011112222";
			int authCode = 123123;
			PhoneServiceRequest request = createPhoneServiceRequest(phoneNumber);

			try (MockedStatic<RandomCodeGenerator> generator = mockStatic(
				RandomCodeGenerator.class)) {
				// Stub
				given(RandomCodeGenerator.generateCode(6)).willReturn(authCode);

				willDoNothing().given(smsSendClient)
					.sendSmsForPhoneAuthentication(anyString(), anyInt());

				// When
				authService.sendSms(request);
			}

			// Then
			ListOperations<String, String> opsForList = redisTemplate.opsForList();
			String savedCode = opsForList.index(authKey, 0);
			String savedStatus = opsForList.index(authKey, 1);

			assertThat(savedCode).isNotNull();
			assertThat(savedStatus).isNotNull();
			assertThat(savedCode).isEqualTo(String.valueOf(authCode));
			assertThat(savedStatus).isEqualTo(Boolean.FALSE.toString());

			then(smsSendClient).should(times(1))
				.sendSmsForPhoneAuthentication(anyString(), anyInt());
		}

		@DisplayName("이미 존재하는 전화번호를 입력하면 예외를 발생시킨다.")
		@Test
		void sendCodeWithExistsPhoneNumber() {

			// Given
			Member member = createMember("01012341234");
			memberRepository.save(member);

			String phoneNumber = member.getPhone();
			PhoneServiceRequest request = createPhoneServiceRequest(phoneNumber);

			// When & Then
			assertThatThrownBy(() -> authService.sendSms(request))
				.isInstanceOf(AlReadyRegisteredPhoneException.class)
				.hasMessage(ALREADY_REGISTERED_PHONE);

			then(smsSendClient).shouldHaveNoInteractions();

			// teardown
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("sms 발송 과정 중 에러가 발생하면 예외를 발생시킨다.")
		@Test
		void sendCodeWithSmsServiceError() {

			// Given
			String phoneNumber = "01011112222";
			PhoneServiceRequest request = createPhoneServiceRequest(phoneNumber);

			// Stub
			SmsSendFailException smsSendFailException = new SmsSendFailException(
				SmsType.PHONE_AUTHENTICATION);
			willThrow(smsSendFailException).given(smsSendClient)
				.sendSmsForPhoneAuthentication(anyString(), anyInt());

			// When & Then
			assertThatThrownBy(() -> authService.sendSms(request))
				.isInstanceOf(SmsSendFailException.class)
				.hasMessage(smsSendFailException.getMessage());

			then(redisTemplate).shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("코드 인증시")
	class authenticateCode {

		private String savedPhoneNumber;
		private String savedAuthKey;
		private String savedAuthCode;

		@BeforeEach
		void setUp() {
			savedPhoneNumber = "01012341234";
			savedAuthKey = "[AUTH]" + savedPhoneNumber;
			savedAuthCode = "123456";

			ListOperations<String, String> opsForList = redisTemplate.opsForList();
			opsForList.rightPush(savedAuthKey, savedAuthCode);
			opsForList.rightPush(savedAuthKey, Boolean.FALSE.toString());
		}

		@AfterEach
		void tearDown() {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
		}

		@DisplayName("올바른 코드를 입력하면 인증이 완료되고, 인증 상태가 redis 에 저장된다.")
		@Test
		void authenticateAuthCode() {

			//Given
			PhoneAuthCodeServiceRequest request = createPhoneAuthCodeServiceRequest(
				savedPhoneNumber, savedAuthCode);

			//When
			authService.authenticateAuthCode(request);

			//Then
			ListOperations<String, String> opsForList = redisTemplate.opsForList();
			String authStatus = opsForList.index(savedAuthKey, 1);

			assertThat(Boolean.valueOf(authStatus)).isTrue();
		}

		@DisplayName("일치하지 않는 코드를 입력하면 예외를 발생시키고, 인증 상태가 변하지 않는다.")
		@Test
		void authenticateAuthCodeWithInvalidAuthCode() {

			//Given
			String invalidAuthCode = "000000";
			PhoneAuthCodeServiceRequest request = createPhoneAuthCodeServiceRequest(
				savedPhoneNumber, invalidAuthCode);
			InvalidPhoneAuthCodeException expectedException = new InvalidPhoneAuthCodeException(
				invalidAuthCode);

			//When & Then
			assertThatThrownBy(() -> authService.authenticateAuthCode(request))
				.isInstanceOf(InvalidPhoneAuthCodeException.class)
				.hasMessage(expectedException.getMessage());

			ListOperations<String, String> opsForList = redisTemplate.opsForList();
			String authStatus = opsForList.index(savedAuthKey, 1);

			assertThat(Boolean.valueOf(authStatus)).isFalse();
		}

		@DisplayName("sms 발송 기록이 없는 전화번호를 입력하면 예외를 발생시킨다.")
		@Test
		void authenticateAuthCodeWithNeverSendSmsPhoneNumber() {

			//Given
			String neverSendSmsPhoneNumber = "01011112222";
			String authCode = "123456";
			PhoneAuthCodeServiceRequest request = createPhoneAuthCodeServiceRequest(
				neverSendSmsPhoneNumber, authCode);
			InvalidPhoneAuthCodeException expectedException = new InvalidPhoneAuthCodeException(
				null);

			//When & Then
			assertThatThrownBy(() -> authService.authenticateAuthCode(request))
				.isInstanceOf(InvalidPhoneAuthCodeException.class)
				.hasMessage(expectedException.getMessage());
		}
	}

	@Nested
	@DisplayName("로그인 시")
	class login {

		String phone = "01012345678";
		String password = "password";
		Member member;

		@AfterEach
		void tearDown() {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
			memberRepository.deleteAllInBatch();
		}

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createEncryptedMember(phone, password));
		}

		@DisplayName("올바른 번호와 비밀번호를 입력하면 로그인에 성공하고, 레디스에 리프레시 토큰이 저장된다")
		@Test
		void loginWithValidRequest() {
			// Given
			LoginServiceRequest loginServiceRequest = createLoginServiceRequest(phone, password);

			// When
			AuthToken authToken = authService.login(loginServiceRequest);
			ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
			String savedToken = stringValueOperations.get(String.valueOf(member.getId()));

			// Then
			assertThat(savedToken).isEqualTo(authToken.getRefreshTokenWithoutPrefix());
		}

		@DisplayName("올바르지 않은 패스워드를 입력하면 예외를 반환한다")
		@Test
		void returnExceptionWithInvalidPassword() {
			// Given
			LoginServiceRequest loginServiceRequest = createLoginServiceRequest(phone, "invalid");

			// When & Then
			assertThatThrownBy(() -> authService.login(loginServiceRequest))
				.isInstanceOf(PasswordMismatchException.class)
				.hasMessage(PASSWORD_MISMATCH);

			then(redisTemplate).shouldHaveNoInteractions();
		}
	}

	private Member createEncryptedMember(String phone, String password) {
		return Member.builder()
			.name("name")
			.nickname("nickname")
			.phone(phone)
			.password(passwordEncryptor.encryptPassword(password))
			.gender(Gender.FEMALE)
			.profileImageUrl("url")
			.build();
	}

	private LoginServiceRequest createLoginServiceRequest(String phone, String password) {
		return LoginServiceRequest.builder()
			.phone(phone)
			.password(password)
			.build();
	}

	private PhoneServiceRequest createPhoneServiceRequest(String phone) {

		return new PhoneServiceRequest(phone);
	}

	private PhoneAuthCodeServiceRequest createPhoneAuthCodeServiceRequest(String phone,
		String code) {

		return new PhoneAuthCodeServiceRequest(phone, code);
	}

	private Member createMember(String phone) {

		return Member.builder()
			.name("name")
			.nickname("nickname")
			.phone(phone)
			.password("password")
			.gender(Gender.FEMALE)
			.profileImageUrl("url")
			.build();
	}
}
