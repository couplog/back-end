package com.dateplan.dateplan.global.interceptor;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Auth;
import com.dateplan.dateplan.global.exception.auth.TokenNotFoundException;
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
		Object handler) {
		Optional<String> tokenByHeader = jwtProvider.resolveToken(request);
		if (tokenByHeader.isEmpty()) {
			throw new TokenNotFoundException(request.getRequestURI());
		}

		String token = tokenByHeader.get().replaceFirst(Auth.BEARER.getContent(), "");
		jwtProvider.checkValidation(token);
		Member member = jwtProvider.findMemberByToken(token);
		MemberThreadLocal.set(member);

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
