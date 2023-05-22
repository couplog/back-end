package com.dateplan.dateplan.global.interceptor;

import static com.dateplan.dateplan.global.exception.ErrorCode.TOKEN_EXPIRED;
import static com.dateplan.dateplan.global.exception.ErrorCode.TOKEN_INVALID;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@RequiredArgsConstructor
@Component
public class AuthInterceptor implements HandlerInterceptor {

	private final JwtProvider jwtProvider;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
		Object handler) throws Exception {
		Optional<String> tokenByHeader = jwtProvider.resolveToken(request);
		if (tokenByHeader.isEmpty()) {
			return true;
		}
		String token = tokenByHeader.get().replaceFirst("Bearer ", "");

		try {
			if (jwtProvider.isValid(token)) {
				Member member = jwtProvider.findMemberByToken(token);
				MemberThreadLocal.set(member);
			}
		} catch (ExpiredJwtException e) {
			throw new ApplicationException(DetailMessage.TOKEN_EXPIRED, TOKEN_EXPIRED);
		} catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
			throw new ApplicationException(DetailMessage.TOKEN_INVALID, TOKEN_INVALID);
		}

		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
		ModelAndView modelAndView) throws Exception {
		Member member = MemberThreadLocal.get();

		if (member == null) {
			return;
		}

		MemberThreadLocal.remove();
	}
}
