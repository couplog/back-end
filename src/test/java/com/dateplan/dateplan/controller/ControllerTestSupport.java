package com.dateplan.dateplan.controller;

import com.dateplan.dateplan.TestRedisConfig;
import com.dateplan.dateplan.domain.anniversary.controller.AnniversaryController;
import com.dateplan.dateplan.domain.anniversary.interceptor.AnniversaryAuthInterceptor;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryReadService;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryService;
import com.dateplan.dateplan.domain.calender.controller.CalenderController;
import com.dateplan.dateplan.domain.calender.service.CalenderReadService;
import com.dateplan.dateplan.domain.couple.controller.CoupleController;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.domain.dating.controller.DatingController;
import com.dateplan.dateplan.domain.dating.service.DatingReadService;
import com.dateplan.dateplan.domain.dating.service.DatingService;
import com.dateplan.dateplan.domain.member.controller.AuthController;
import com.dateplan.dateplan.domain.member.controller.MemberController;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.domain.schedule.controller.ScheduleController;
import com.dateplan.dateplan.domain.schedule.service.ScheduleReadService;
import com.dateplan.dateplan.domain.schedule.service.ScheduleService;
import com.dateplan.dateplan.domain.sms.service.SmsSendClient;
import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.interceptor.AuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@WebMvcTest(controllers = {AuthController.class, MemberController.class, CoupleController.class,
	AnniversaryController.class, ScheduleController.class, DatingController.class,
	CalenderController.class})
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
	protected AnniversaryAuthInterceptor anniversaryAuthInterceptor;

	@MockBean
	protected CoupleService coupleService;

	@MockBean
	protected CoupleReadService coupleReadService;

	@MockBean
	protected ScheduleService scheduleService;

	@MockBean
	protected AnniversaryService anniversaryService;

	@MockBean
	protected ScheduleReadService scheduleReadService;

	@MockBean
	protected AnniversaryReadService anniversaryReadService;

	@MockBean
	protected DatingService datingService;

	@MockBean
	protected DatingReadService datingReadService;

	@MockBean
	protected CalenderReadService calenderReadService;
}
