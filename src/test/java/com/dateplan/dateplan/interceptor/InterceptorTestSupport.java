package com.dateplan.dateplan.interceptor;

import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class InterceptorTestSupport {

	@Autowired
	protected AuthInterceptor authInterceptor;

	@MockBean
	protected JwtProvider jwtProvider;

	@Mock
	protected HttpServletRequest request;

	@Mock
	protected HttpServletResponse response;

	@Mock
	protected Object handler;

}
