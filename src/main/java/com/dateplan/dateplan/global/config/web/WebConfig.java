package com.dateplan.dateplan.global.config.web;

import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final JwtProvider jwtProvider;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry
			.addInterceptor(new AuthInterceptor(jwtProvider))
			.addPathPatterns("/**")
			.excludePathPatterns("/api/auth/**");
	}
}
