package com.dateplan.dateplan.service.member;

import static com.dateplan.dateplan.global.constant.Auth.*;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dateplan.dateplan.domain.member.dto.AuthToken;
import com.dateplan.dateplan.domain.member.dto.LoginServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.auth.PasswordMismatchException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import org.jasypt.util.password.PasswordEncryptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
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

			// Then
			ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
			assertThat(stringValueOperations.get(
				String.valueOf(member.getId()).replaceAll(BEARER.getContent(), "")))
				.isEqualTo(authToken.getRefreshToken());
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

}
