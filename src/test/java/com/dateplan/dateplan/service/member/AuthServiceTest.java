package com.dateplan.dateplan.service.member;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_REGISTERED_PHONE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.MEMBER_NOT_FOUND;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.NOT_AUTHENTICATED_PHONE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.PASSWORD_MISMATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.member.service.dto.request.LoginServiceRequest;
import com.dateplan.dateplan.domain.member.service.dto.response.LoginServiceResponse;
import com.dateplan.dateplan.domain.member.dto.signup.PhoneAuthCodeServiceRequest;
import com.dateplan.dateplan.domain.member.dto.signup.PhoneServiceRequest;
import com.dateplan.dateplan.domain.member.dto.signup.SendSmsServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.domain.sms.type.SmsType;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.auth.InvalidPhoneAuthCodeException;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.global.exception.auth.PasswordMismatchException;
import com.dateplan.dateplan.global.exception.auth.PhoneAuthLimitOverException;
import com.dateplan.dateplan.global.exception.auth.PhoneNotAuthenticatedException;
import com.dateplan.dateplan.global.exception.member.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.global.exception.sms.SmsSendFailException;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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

	@SpyBean
	private StringRedisTemplate redisTemplate;

	@Autowired
	private CoupleRepository coupleRepository;

	@Nested
	@DisplayName("코드 전송시")
	class sendCode {

		@AfterEach
		void tearDown() {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
		}

		@DisplayName("유효한 전화번호를 입력하면 인증 코드가 발송되고, 인증 상태와 인증 횟수가 redis 에 저장된다.")
		@Test
		void sendCodeWithValidPhoneNumber() {

			// Given
			String phoneNumber = "01011112222";
			String authKey = "[AUTH]" + phoneNumber;
			String requestCountKey = "[AUTH_COUNT]" + phoneNumber;
			int authCode = 123123;
			PhoneServiceRequest request = createPhoneServiceRequest(phoneNumber);

			try (MockedStatic<RandomCodeGenerator> generator = mockStatic(
				RandomCodeGenerator.class)) {

				// Stub
				given(RandomCodeGenerator.generateCode(6))
					.willReturn(authCode);

				willDoNothing()
					.given(smsSendClient)
					.sendSmsForPhoneAuthentication(anyString(), anyInt());

				// When
				SendSmsServiceResponse response = authService.sendSms(request);
				assertThat(response.getCurrentCount()).isOne();
			}

			// Then
			ListOperations<String, String> opsForList = redisTemplate.opsForList();
			String savedCode = opsForList.index(authKey, 0);
			String savedStatus = opsForList.index(authKey, 1);
			String savedRequestCount = redisTemplate.opsForValue().get(requestCountKey);

			assertThat(savedCode)
				.isNotNull()
				.isEqualTo(String.valueOf(authCode));
			assertThat(savedStatus)
				.isNotNull()
				.isEqualTo(Boolean.FALSE.toString());
			assertThat(savedRequestCount)
				.isNotNull()
				.isEqualTo(String.valueOf(1));

			then(smsSendClient)
				.should(times(1))
				.sendSmsForPhoneAuthentication(anyString(), anyInt());
		}

		@DisplayName("이미 존재하는 전화번호를 입력하면 예외를 발생시킨다.")
		@Test
		void sendCodeWithExistsPhoneNumber() {

			// Given
			Member member = createMember("01012341234", "password", "nickname");
			memberRepository.save(member);

			String phoneNumber = member.getPhone();
			PhoneServiceRequest request = createPhoneServiceRequest(phoneNumber);

			// When & Then
			assertThatThrownBy(() -> authService.sendSms(request))
				.isInstanceOf(AlReadyRegisteredPhoneException.class)
				.hasMessage(ALREADY_REGISTERED_PHONE);

			then(redisTemplate)
				.shouldHaveNoInteractions();
			then(smsSendClient)
				.shouldHaveNoInteractions();

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
			willThrow(smsSendFailException)
				.given(smsSendClient)
				.sendSmsForPhoneAuthentication(anyString(), anyInt());

			// When & Then
			assertThatThrownBy(() -> authService.sendSms(request))
				.isInstanceOf(SmsSendFailException.class)
				.hasMessage(smsSendFailException.getMessage());

			then(redisTemplate)
				.should(never())
				.opsForList();
			then(redisTemplate)
				.should(times(1))
				.opsForValue();
		}

		@DisplayName("요청 횟수를 초과하면 예외를 발생시킨다.")
		@Test
		void sendCodeWithOverRequestLimitCount() {

			// Given
			String phoneNumber = "01011112222";
			String requestCountKey = "[AUTH_COUNT]" + phoneNumber;
			PhoneServiceRequest request = createPhoneServiceRequest(phoneNumber);

			ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
			opsForValue.set(requestCountKey, "5");

			// When & Then
			assertThatThrownBy(() -> authService.sendSms(request))
				.isInstanceOf(PhoneAuthLimitOverException.class)
				.hasMessage(DetailMessage.PHONE_AUTH_LIMIT_OVER);

			then(redisTemplate)
				.should(never())
					.opsForList();
			then(redisTemplate)
				.should(times(2))
				.opsForValue();
			then(smsSendClient)
				.shouldHaveNoInteractions();
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
			PhoneAuthCodeServiceRequest request =
				createPhoneAuthCodeServiceRequest(savedPhoneNumber, invalidAuthCode);
			InvalidPhoneAuthCodeException expectedException =
				new InvalidPhoneAuthCodeException(invalidAuthCode);

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
			PhoneAuthCodeServiceRequest request =
				createPhoneAuthCodeServiceRequest(neverSendSmsPhoneNumber, authCode);
			InvalidPhoneAuthCodeException expectedException =
				new InvalidPhoneAuthCodeException(null);

			//When & Then
			assertThatThrownBy(() -> authService.authenticateAuthCode(request))
				.isInstanceOf(InvalidPhoneAuthCodeException.class)
				.hasMessage(expectedException.getMessage());
		}
	}

	@Nested
	@DisplayName("전화번호 인증 여부 확인시")
	class ThrowIfPhoneNotAuthenticated {

		@AfterEach
		void tearDown() {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
		}

		@DisplayName("요청한 전화번호가 인증 성공 상태라면 예외를 발생시키지 않는다.")
		@Test
		void withAuthenticatedPhone() {

			// Given
			String authenticatedPhoneNumber = "01012341234";
			String authKey = "[AUTH]" + authenticatedPhoneNumber;
			String authCode = "123456";

			ListOperations<String, String> opsForList = redisTemplate.opsForList();
			opsForList.rightPush(authKey, authCode);
			opsForList.rightPush(authKey, Boolean.TRUE.toString());

			// When & Then
			assertThatNoException()
				.isThrownBy(
					() -> authService.throwIfPhoneNotAuthenticated(authenticatedPhoneNumber));
		}

		@DisplayName("요청한 전화번호의 인증 기록이 없다면 예외를 발생시킨다.")
		@Test
		void withNeverAuthenticatedPhone() {

			// Given
			String neverAuthenticatedPhone = "01012341234";

			// When & Then
			assertThatThrownBy(
				() -> authService.throwIfPhoneNotAuthenticated(neverAuthenticatedPhone))
				.isInstanceOf(PhoneNotAuthenticatedException.class)
				.hasMessage(NOT_AUTHENTICATED_PHONE);
		}

		@DisplayName("요청한 전화번호의 인증 기록은 있지만, 인증 성공 상태가 아니라면 예외를 발생시킨다.")
		@Test
		void withNotAuthenticatedPhone() {

			// Given
			String notAuthenticatedPhoneNumber = "01012341234";
			String authKey = "[AUTH]" + notAuthenticatedPhoneNumber;
			String authCode = "123456";

			ListOperations<String, String> opsForList = redisTemplate.opsForList();
			opsForList.rightPush(authKey, authCode);
			opsForList.rightPush(authKey, Boolean.FALSE.toString());

			// When & Then
			assertThatThrownBy(
				() -> authService.throwIfPhoneNotAuthenticated(notAuthenticatedPhoneNumber))
				.isInstanceOf(PhoneNotAuthenticatedException.class)
				.hasMessage(NOT_AUTHENTICATED_PHONE);
		}
	}

	@Nested
	@DisplayName("인증 정보 삭제시")
	class DeleteAuthenticationInfoInRedis {

		String authenticatedPhoneNumber = "01012341234";
		String authKey = "[AUTH]" + authenticatedPhoneNumber;

		@BeforeEach
		void setUp() {
			String authCode = "123456";

			ListOperations<String, String> opsForList = redisTemplate.opsForList();
			opsForList.rightPush(authKey, authCode);
			opsForList.rightPush(authKey, Boolean.TRUE.toString());
		}

		@AfterEach
		void tearDown() {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
		}

		@DisplayName("내부에 인증되어 있는 번호로 요청하면, 주어진 번호의 인증 기록이 삭제된다.")
		@Test
		void withAuthenticatedPhone() {

			// When
			authService.deleteAuthenticationInfoInRedis(authenticatedPhoneNumber);

			// Then
			ListOperations<String, String> opsForList = redisTemplate.opsForList();
			Long size = opsForList.size(authKey);
			assertThat(size).isZero();
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
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember(phone, password, "nickname1"));
		}

		@DisplayName("올바른 번호와 비밀번호를 입력하면 로그인에 성공하고, 레디스에 리프레시 토큰이 저장된다")
		@Test
		void loginWithValidRequest() {
			// Given
			LoginServiceRequest loginServiceRequest = createLoginServiceRequest(phone, password);

			// When
			LoginServiceResponse response = authService.login(loginServiceRequest);
			ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
			String savedToken = stringValueOperations.get("[REFRESH]" + member.getId());

			// Then
			assertThat(savedToken)
				.isEqualTo(response.getAuthToken().getRefreshTokenWithoutPrefix());
		}

		@DisplayName("올바른 번호와 비밀번호를 입력하면 로그인에 성공하고, 커플 연결 여부를 반환한다, 연결이 되었을 때")
		@Test
		void loginWithValidRequestAndConnected() {

			// Given
			String phone2 = "01012345679";

			Member member2 = memberRepository.save(createMember(phone2, password, "nickname2"));

			LoginServiceRequest member1Request = createLoginServiceRequest(phone, password);
			LoginServiceRequest member2Request = createLoginServiceRequest(phone2, password);

			Couple couple = Couple.builder()
				.member1(member)
				.member2(member2)
				.firstDate(LocalDate.now().minusDays(1L))
				.build();
			coupleRepository.save(couple);

			// When
			LoginServiceResponse member1Response = authService.login(member1Request);
			LoginServiceResponse member2Response = authService.login(member2Request);

			// Then
			assertThat(member1Response.getIsConnected()).isTrue();
			assertThat(member2Response.getIsConnected()).isTrue();

		}

		@DisplayName("올바른 번호와 비밀번호를 입력하면 로그인에 성공하고, 커플 연결 여부를 반환한다, 연결이 되지 않았을 때")
		@Test
		void loginWithValidRequestAndDisconnected() {

			// Given
			String phone2 = "01012345679";

			memberRepository.save(createMember(phone2, password, "nickname2"));

			LoginServiceRequest member1Request = createLoginServiceRequest(phone, password);
			LoginServiceRequest member2Request = createLoginServiceRequest(phone2, password);

			// When
			LoginServiceResponse member1Response = authService.login(member1Request);
			LoginServiceResponse member2Response = authService.login(member2Request);

			// Then
			assertThat(member1Response.getIsConnected()).isFalse();
			assertThat(member2Response.getIsConnected()).isFalse();

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

			then(redisTemplate)
				.shouldHaveNoInteractions();
		}

		@DisplayName("전화번호를 입력하지 않으면 실패한다")
		@NullAndEmptySource
		@ParameterizedTest
		void failWithPhoneIsNull(String phone) {
			// Given
			LoginServiceRequest request = LoginServiceRequest.builder()
				.phone(phone)
				.password(password)
				.build();

			// When & Then
			assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(MemberNotFoundException.class)
				.isNotInstanceOf(NullPointerException.class)
				.hasMessage(MEMBER_NOT_FOUND);

			then(redisTemplate)
				.shouldHaveNoInteractions();
		}

		@DisplayName("비밀번호를 입력하지 않으면 실패한다")
		@NullAndEmptySource
		@ParameterizedTest
		void failWithPasswordIsNull(String password) {
			// Given
			LoginServiceRequest request = LoginServiceRequest.builder()
				.phone(phone)
				.password(password)
				.build();

			// When & Then
			assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(PasswordMismatchException.class)
				.isNotInstanceOf(NullPointerException.class)
				.hasMessage(PASSWORD_MISMATCH);

			then(redisTemplate)
				.shouldHaveNoInteractions();
		}
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

	private Member createMember(String phone, String password, String nickname) {

		return Member.builder()
			.name("name")
			.nickname(nickname)
			.phone(phone)
			.password(password)
			.gender(Gender.FEMALE)
			.birthDay(LocalDate.of(2020, 10, 10))
			.build();
	}
}
