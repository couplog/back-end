package com.dateplan.dateplan.service.member;

import static com.dateplan.dateplan.global.constant.Auth.ACCESS_TOKEN_EXPIRATION;
import static com.dateplan.dateplan.global.constant.Auth.REFRESH_TOKEN_EXPIRATION;
import static com.dateplan.dateplan.global.constant.Auth.SUBJECT_ACCESS_TOKEN;
import static com.dateplan.dateplan.global.constant.Auth.SUBJECT_REFRESH_TOKEN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.MEMBER_NOT_FOUND;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.TOKEN_EXPIRED;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.TOKEN_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dateplan.dateplan.domain.member.dto.AuthToken;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.global.exception.auth.TokenExpiredException;
import com.dateplan.dateplan.global.exception.auth.TokenInvalidException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class JwtProviderTest extends ServiceTestSupport {

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private MemberRepository memberRepository;

	@SpyBean
	private StringRedisTemplate redisTemplate;

	@AfterEach
	void tearDown() {
		memberRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("토큰이 주어졌을 때")
	class givenToken {

		@DisplayName("올바른 엑세스 토큰이 주어지면 올바른 유저를 반환한다")
		@Test
		void returnMemberGivenValidToken() {

			// Given
			Member member = createMember("01012345678");
			memberRepository.save(member);

			// When
			String accessToken = jwtProvider.generateToken(
				member.getId(),
				ACCESS_TOKEN_EXPIRATION.getExpiration(),
				SUBJECT_ACCESS_TOKEN.getContent()
			);

			// Then
			assertThat(jwtProvider.findMemberByToken(accessToken).getId())
				.isEqualTo(member.getId());
		}

		@DisplayName("올바른 리스레시 토큰이 주어지면 올바른 유저를 반환한다")
		@Test
		void returnMemberGivenValidRefreshToken() {

			// Given
			Member member = createMember("01012345678");
			memberRepository.save(member);

			// When
			String refreshToken = jwtProvider.generateToken(
				member.getId(),
				REFRESH_TOKEN_EXPIRATION.getExpiration(),
				SUBJECT_REFRESH_TOKEN.getContent()
			);

			// Then
			assertThat(jwtProvider.findMemberByToken(refreshToken).getId())
				.isEqualTo(member.getId());
		}

		@DisplayName("유저가 존재하지 않으면 예외를 반환한다")
		@Test
		void returnNotFoundExceptionGivenInvalidToken() {

			// Given
			Member member = createMember("01012345678");
			memberRepository.save(member);

			// When
			String accessToken = jwtProvider.generateToken(
				member.getId() + 1,
				ACCESS_TOKEN_EXPIRATION.getExpiration(),
				SUBJECT_ACCESS_TOKEN.getContent()
			);

			// Then
			assertThatThrownBy(() -> jwtProvider.findMemberByToken(accessToken))
				.isInstanceOf(MemberNotFoundException.class)
				.hasMessage(MEMBER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("토큰 생성 시")
	class generateToken {

		String savedRefreshToken;
		Member member;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("01012345678"));
			savedRefreshToken = jwtProvider.generateToken(member.getId(),
				REFRESH_TOKEN_EXPIRATION.getExpiration(),
				SUBJECT_REFRESH_TOKEN.getContent());

			ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
			stringValueOperations.set(String.valueOf(member.getId()), savedRefreshToken);
		}

		@AfterEach
		void tearDown() {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
		}

		@DisplayName("올바른 값이 주어지면 토큰을 반환한다")
		@Test
		void returnValidToken() {
			// Given & When
			String accessToken = jwtProvider.generateToken(member.getId(),
				ACCESS_TOKEN_EXPIRATION.getExpiration(), SUBJECT_ACCESS_TOKEN.getContent());
			String refreshToken = jwtProvider.generateToken(member.getId(),
				REFRESH_TOKEN_EXPIRATION.getExpiration(), SUBJECT_REFRESH_TOKEN.getContent());

			// Then
			assertThat(accessToken).isNotNull();
			assertThat(accessToken).isNotEmpty();
			assertThat(refreshToken).isNotNull();
			assertThat(refreshToken).isNotEmpty();
		}

		@DisplayName("올바른 리프레시 토큰이 주어지면 엑세스 토큰을 반환한다")
		@Test
		void returnAccessTokenGivenRefreshToken() {
			AuthToken authToken = jwtProvider.generateTokenByRefreshToken(savedRefreshToken);

			String accessToken = authToken.getAccessToken().replaceAll("Bearer ", "");
			String refreshToken = authToken.getRefreshToken().replaceAll("Bearer ", "");

			assertThat(
				jwtProvider.findMemberByToken(accessToken).getId()).isEqualTo(member.getId());
			assertThat(
				jwtProvider.findMemberByToken(refreshToken).getId()).isEqualTo(member.getId());
		}

		@DisplayName("올바르지 않은 리프레시 토큰이 주어지면 예외를 반환한다")
		@Test
		void returnExceptionGivenInvalidRefreshToken() {
			// Given
			String refreshToken = "this_is_invalid_token";

			// When & Then
			assertThatThrownBy(() -> jwtProvider.generateTokenByRefreshToken(refreshToken))
				.isInstanceOf(TokenInvalidException.class)
				.hasMessage(TOKEN_INVALID);

		}
	}

	@Nested
	@DisplayName("토큰 유효성 검사 시")
	class validationToken {

		@DisplayName("올바른 토큰이 주어지면 true를 반환한다.")
		@Test
		void returnTrue() {
			String token = jwtProvider.generateToken(1L, ACCESS_TOKEN_EXPIRATION.getExpiration(),
				SUBJECT_ACCESS_TOKEN.getContent());

			assertThat(jwtProvider.isValid(token)).isTrue();
		}

		@DisplayName("만료된 토큰이 주어지면 예외를 반환한다")
		@Test
		void returnExpiredExceptionGivenExpiredToken() {
			String token = jwtProvider.generateToken(
				1L,
				1L,
				SUBJECT_ACCESS_TOKEN.getContent()
			);

			assertThatThrownBy(() -> jwtProvider.findMemberByToken(token))
				.isInstanceOf(TokenExpiredException.class)
				.hasMessage(TOKEN_EXPIRED);
		}

		@DisplayName("유효하지 않은 토큰이 주어지면 예외를 반환한다")
		@Test
		void returnInvalidExceptionGivenInvalidToken() {
			String token = "this_is_invalid_token";

			assertThatThrownBy(() -> jwtProvider.findMemberByToken(token))
				.isInstanceOf(TokenInvalidException.class)
				.hasMessage(TOKEN_INVALID);
		}
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
