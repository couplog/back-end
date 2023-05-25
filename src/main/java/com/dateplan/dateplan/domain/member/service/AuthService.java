package com.dateplan.dateplan.domain.member.service;

import static com.dateplan.dateplan.global.constant.Auth.ACCESS_TOKEN_EXPIRATION;
import static com.dateplan.dateplan.global.constant.Auth.BEARER;
import static com.dateplan.dateplan.global.constant.Auth.HEADER_AUTHORIZATION;
import static com.dateplan.dateplan.global.constant.Auth.HEADER_REFRESH_TOKEN;
import static com.dateplan.dateplan.global.constant.Auth.REFRESH_TOKEN_EXPIRATION;
import static com.dateplan.dateplan.global.constant.Auth.SUBJECT_ACCESS_TOKEN;
import static com.dateplan.dateplan.global.constant.Auth.SUBJECT_REFRESH_TOKEN;

import com.dateplan.dateplan.domain.member.dto.AuthToken;
import com.dateplan.dateplan.domain.member.dto.LoginServiceRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneAuthCodeServiceRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.sms.service.SmsService;
import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.exception.InvalidPhoneAuthCodeException;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.global.exception.auth.PasswordMismatchException;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String AUTH_KEY_PREFIX = "[AUTH]";

	private final MemberReadService memberReadService;
	private final SmsService smsService;
	private final StringRedisTemplate redisTemplate;
	private final PasswordEncryptor passwordEncryptor;
	private final MemberRepository memberRepository;
	private final JwtProvider jwtProvider;

	public void sendSms(PhoneServiceRequest request) {

		int code = RandomCodeGenerator.generateCode(6);
		String phone = request.getPhone();

		memberReadService.throwIfPhoneExists(phone);
		//smsService.sendSmsForPhoneAuthentication(phone, code);

		saveAuthCodeInRedis(phone, code);
	}

	public void authenticateAuthCode(PhoneAuthCodeServiceRequest request) {

		ListOperations<String, String> opsForList = redisTemplate.opsForList();

		String phone = request.getPhone();
		String key = AUTH_KEY_PREFIX + phone;
		String savedCode = opsForList.index(key, 0);

		validateCode(savedCode, request.getCode());

		opsForList.rightPop(key);
		opsForList.rightPush(key, Boolean.TRUE.toString());
	}

	private void saveAuthCodeInRedis(String phone, int code) {

		ListOperations<String, String> opsForList = redisTemplate.opsForList();

		String key = AUTH_KEY_PREFIX + phone;

		redisTemplate.delete(key);

		opsForList.rightPush(key, String.valueOf(code));
		opsForList.rightPush(key, String.valueOf(false));
		redisTemplate.expire(key, 2, TimeUnit.MINUTES);
	}

	private void validateCode(String code, String input) {

		if (code == null || !Objects.equals(input, code)) {
			throw new InvalidPhoneAuthCodeException(code);
		}
	}

	public void login(LoginServiceRequest request, HttpServletResponse response) {
		Member member = memberRepository.findByPhone(request.getPhone())
			.orElseThrow(MemberNotFoundException::new);

		if (mismatchPassword(request, member)) {
			throw new PasswordMismatchException();
		}

		String accessToken = BEARER.getContent() + jwtProvider.generateToken(
			member.getId(),
			ACCESS_TOKEN_EXPIRATION.getExpiration(),
			SUBJECT_ACCESS_TOKEN.getContent());
		String refreshToken = BEARER.getContent() + jwtProvider.generateToken(
			member.getId(),
			REFRESH_TOKEN_EXPIRATION.getExpiration(),
			SUBJECT_REFRESH_TOKEN.getContent());

		response.setHeader(HEADER_AUTHORIZATION.getContent(), accessToken);
		response.setHeader(HEADER_REFRESH_TOKEN.getContent(), refreshToken);

		ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
		String key = String.valueOf(member.getId());

		stringValueOperations.set(key, refreshToken);
	}

	private boolean mismatchPassword(LoginServiceRequest request, Member member) {
		return !passwordEncryptor.checkPassword(request.getPassword(), member.getPassword());
	}

	public void refreshAccessToken(String refreshToken, HttpServletResponse response) {
		AuthToken authToken = jwtProvider.generateTokenByRefreshToken(refreshToken);

		response.setHeader(HEADER_AUTHORIZATION.getContent(), authToken.getAccessToken());
		response.setHeader(HEADER_REFRESH_TOKEN.getContent(), authToken.getRefreshToken());
	}
}