package com.dateplan.dateplan.controller;

import com.dateplan.dateplan.domain.couple.controller.CoupleController;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.domain.member.controller.AuthController;
import com.dateplan.dateplan.domain.member.controller.MemberController;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.domain.sms.service.SmsSendClient;
import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.interceptor.AuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(controllers = {AuthController.class, MemberController.class, CoupleController.class})
public abstract class ControllerTestSupport {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper om;

	@MockBean
	protected AuthService authService;

	@MockBean
	protected MemberService memberService;

	@MockBean
	protected MemberReadService memberReadService;

	@MockBean
	protected SmsSendClient smsSendClient;

	@MockBean
	protected JwtProvider jwtProvider;

	@MockBean
	protected AuthInterceptor authInterceptor;

	@MockBean
	protected CoupleService coupleService;

	@MockBean
	protected CoupleReadService coupleReadService;
}
