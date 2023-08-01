package com.dateplan.dateplan.global.config.web;

import com.dateplan.dateplan.domain.anniversary.interceptor.AnniversaryAuthInterceptor;
import com.dateplan.dateplan.domain.dating.interceptor.DatingAuthInterceptor;
import com.dateplan.dateplan.global.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final AuthInterceptor authInterceptor;
	private final AnniversaryAuthInterceptor anniversaryAuthInterceptor;
	private final DatingAuthInterceptor datingAuthInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry
			.addInterceptor(authInterceptor)
			.addPathPatterns("/**")
			.excludePathPatterns("/api/auth/**");

		registry
			.addInterceptor(anniversaryAuthInterceptor)
			.addPathPatterns("/api/couples/**/anniversary/**");

		registry
			.addInterceptor(datingAuthInterceptor)
			.addPathPatterns("/api/couples/**/dating/**");
	}
}
