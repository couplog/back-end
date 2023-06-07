package com.dateplan.dateplan.interceptor.auth;


import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.TOKEN_EXPIRED;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.TOKEN_INVALID;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.TOKEN_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.auth.TokenExpiredException;
import com.dateplan.dateplan.global.exception.auth.TokenInvalidException;
import com.dateplan.dateplan.global.exception.auth.TokenNotFoundException;
import com.dateplan.dateplan.global.interceptor.AuthInterceptor;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
public class AuthInterceptorTest {

	private final AuthInterceptor authInterceptor;
	private final JwtProvider jwtProvider;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private Object handler;

	@Mock
	private ModelAndView mav;

	public AuthInterceptorTest() {
		this.jwtProvider = mock(JwtProvider.class);
		this.authInterceptor = new AuthInterceptor(jwtProvider);
	}

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

			then(jwtProvider).should(never()).isValid(anyString());
			then(jwtProvider).should(never()).findMemberByToken(anyString());
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

	@DisplayName("postHandle 호출 시")
	@Nested
	class InvokePostHandle {

		@DisplayName("Member 객체가 존재한다면 삭제한다.")
		@Test
		void removeMember() throws Exception {

			try (MockedStatic<MemberThreadLocal> generator = mockStatic(MemberThreadLocal.class)) {

				// Stub
				given(MemberThreadLocal.get())
					.willReturn(createMember());

				// When
				authInterceptor.postHandle(request, response, handler, mav);

				// Verify
				generator.verify(MemberThreadLocal::remove, times(1));
			}

			// Then
			assertThat(MemberThreadLocal.get()).isNull();
		}

		@DisplayName("Member 객체가 존재하지 않는다면 바로 리턴한다")
		@Test
		void returnWithoutRemove() throws Exception {

			try (MockedStatic<MemberThreadLocal> generator = mockStatic(MemberThreadLocal.class)) {

				// Stub
				given(MemberThreadLocal.get())
					.willReturn(null);

				// When
				authInterceptor.postHandle(request, response, handler, mav);

				// Verify
				generator.verify(MemberThreadLocal::remove, never());
			}

			// Then
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
