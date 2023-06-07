package com.dateplan.dateplan.interceptor.auth;


import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.auth.TokenExpiredException;
import com.dateplan.dateplan.global.exception.auth.TokenInvalidException;
import com.dateplan.dateplan.global.exception.auth.TokenNotFoundException;
import com.dateplan.dateplan.interceptor.InterceptorTestSupport;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AuthInterceptorTest extends InterceptorTestSupport {

	@DisplayName("인터셉터에 요청이 들어왔을 때")
	@Nested
	class InterceptorRequested {

		String token = "token";

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
		}

		@DisplayName("올바른 요청이면 ThreadLocal에 멤버가 set되고, true를 반환한다")
		@Test
		void validRequest() throws Exception {

			// Given
			Member member = createMember();

			// Stub
			given(jwtProvider.resolveToken(any(HttpServletRequest.class)))
				.willReturn(Optional.of(token));
			given(jwtProvider.isValid(anyString()))
				.willReturn(true);
			given(jwtProvider.findMemberByToken(anyString()))
				.willReturn(member);

			// When
			boolean result = authInterceptor.preHandle(request, response, handler);

			// Then
			assertThat(MemberThreadLocal.get()).isEqualTo(member);
			assertThat(result).isTrue();
		}

		@DisplayName("토큰이 존재하지 않으면 예외를 반환한다")
		@Test
		void tokenNotFoundRequest() {

			// Stub
			given(jwtProvider.resolveToken(any(HttpServletRequest.class)))
				.willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
				.isInstanceOf(TokenNotFoundException.class)
				.hasMessage(TOKEN_NOT_FOUND);
			assertThat(MemberThreadLocal.get()).isNull();

//			then(jwtProvider.isValid(anyString()))
//				.shouldHaveNoInteractions();
//			then(jwtProvider.findMemberByToken(anyString()))
//				.shouldHaveNoInteractions();
		}

		@DisplayName("토큰이 만료되면 예외를 반환한다")
		@Test
		void tokenExpiredRequest() {

			// Stub
			given(jwtProvider.resolveToken(any(HttpServletRequest.class)))
				.willReturn(Optional.of(token));
			given(jwtProvider.isValid(anyString()))
				.willThrow(ExpiredJwtException.class);

			// When & Then
			assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
				.isInstanceOf(TokenExpiredException.class)
				.hasMessage(TOKEN_EXPIRED);
			assertThat(MemberThreadLocal.get()).isNull();

		}

		@DisplayName("토큰이 유효하지 않으면 예외를 반환한다")
		@Test
		void tokenInvalidRequest() {

			// Stub
			given(jwtProvider.resolveToken(any(HttpServletRequest.class)))
				.willReturn(Optional.of(token));
			given(jwtProvider.isValid(anyString()))
				.willThrow(MalformedJwtException.class);

			// When & Then
			assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
				.isInstanceOf(TokenInvalidException.class)
				.hasMessage(TOKEN_INVALID);
			assertThat(MemberThreadLocal.get()).isNull();
		}
	}

	private Member createMember() {
		return Member.builder()
			.name("name")
			.password("password")
			.phone("01012345678")
			.nickname("nickname")
			.gender(Gender.MALE)
			.birth(LocalDate.now().minusDays(1))
			.build();
	}
}
