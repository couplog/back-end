package com.dateplan.dateplan.domain.member.service;

import static com.dateplan.dateplan.global.constant.Auth.ACCESS_TOKEN_EXPIRATION;
import static com.dateplan.dateplan.global.constant.Auth.BEARER;
import static com.dateplan.dateplan.global.constant.Auth.REFRESH_TOKEN_EXPIRATION;
import static com.dateplan.dateplan.global.constant.Auth.SUBJECT_ACCESS_TOKEN;
import static com.dateplan.dateplan.global.constant.Auth.SUBJECT_REFRESH_TOKEN;

import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.dto.AuthToken;
import com.dateplan.dateplan.domain.member.dto.LoginServiceResponse;
import com.dateplan.dateplan.domain.member.dto.LoginServiceRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneAuthCodeServiceRequest;
import com.dateplan.dateplan.domain.member.dto.PhoneServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.sms.service.SmsSendClient;
import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.exception.InvalidPhoneAuthCodeException;
import com.dateplan.dateplan.global.exception.PhoneNotAuthenticatedException;
import com.dateplan.dateplan.global.exception.auth.PasswordMismatchException;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
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
	private final SmsSendClient smsSendClient;
	private final StringRedisTemplate redisTemplate;
	private final PasswordEncryptor passwordEncryptor;
	private final JwtProvider jwtProvider;
	private final CoupleReadService coupleReadService;

	public void sendSms(PhoneServiceRequest request) {

		int code = RandomCodeGenerator.generateCode(6);
		String phone = request.getPhone();

		memberReadService.throwIfPhoneExists(phone);
		smsSendClient.sendSmsForPhoneAuthentication(phone, code);

		saveAuthCodeInRedis(phone, code);
	}

	public void authenticateAuthCode(PhoneAuthCodeServiceRequest request) {

		ListOperations<String, String> opsForList = redisTemplate.opsForList();

		String phone = request.getPhone();
		String key = getAuthKey(phone);
		String savedCode = opsForList.index(key, 0);

		validateCode(savedCode, request.getCode());

		opsForList.rightPop(key);
		opsForList.rightPush(key, Boolean.TRUE.toString());
	}

	public void throwIfPhoneNotAuthenticated(String phone) {

		ListOperations<String, String> opsForList = redisTemplate.opsForList();
		String key = getAuthKey(phone);
		String status = opsForList.index(key, 1);

		if (!Boolean.parseBoolean(status)) {
			throw new PhoneNotAuthenticatedException();
		}
	}

	public void deleteAuthenticationInfoInRedis(String phone) {

		String key = getAuthKey(phone);

		redisTemplate.delete(key);
	}

	private void saveAuthCodeInRedis(String phone, int code) {

		ListOperations<String, String> opsForList = redisTemplate.opsForList();

		String key = getAuthKey(phone);

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

	private String getAuthKey(String phone) {

		return AUTH_KEY_PREFIX + phone;
	}

	public LoginServiceResponse login(LoginServiceRequest request) {
		Member member = memberReadService.findMemberByPhoneOrElseThrow(request.getPhone());

		if (mismatchPassword(request, member)) {
			throw new PasswordMismatchException();
		}

		AuthToken authToken = createAuthToken(member);
		saveRefreshTokenInRedis(member, authToken.getRefreshTokenWithoutPrefix());
		boolean isConnected = coupleReadService.isMemberConnected(member);
		return LoginServiceResponse.builder()
			.authToken(authToken)
			.isConnected(isConnected)
			.build();
	}

	private AuthToken createAuthToken(Member member) {
		String accessToken = BEARER.getContent() + jwtProvider.generateToken(
			member.getId(),
			ACCESS_TOKEN_EXPIRATION.getExpiration(),
			SUBJECT_ACCESS_TOKEN.getContent());
		String refreshToken = BEARER.getContent() + jwtProvider.generateToken(
			member.getId(),
			REFRESH_TOKEN_EXPIRATION.getExpiration(),
			SUBJECT_REFRESH_TOKEN.getContent());
		return AuthToken.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	private void saveRefreshTokenInRedis(Member member, String refreshToken) {
		ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
		String key = String.valueOf(member.getId());

		stringValueOperations.set(key, refreshToken);
	}

	private boolean mismatchPassword(LoginServiceRequest request, Member member) {

		return !passwordEncryptor.checkPassword(request.getPassword(), member.getPassword());
	}

	public AuthToken refreshAccessToken(String refreshToken) {
		return jwtProvider.generateTokenByRefreshToken(refreshToken);
	}
}