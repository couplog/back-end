package com.dateplan.dateplan.domain.member.service;

import com.dateplan.dateplan.domain.member.dto.PhoneServiceRequest;
import com.dateplan.dateplan.domain.sms.service.SmsService;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String AUTH_KEY_PREFIX = "[AUTH]";

	private final MemberReadService memberReadService;
	private final SmsService smsService;
	private final StringRedisTemplate redisTemplate;

	public void sendSms(PhoneServiceRequest request) {

		int code = RandomCodeGenerator.generateCode(6);
		String phone = request.getPhone();

		memberReadService.throwIfPhoneExists(phone);
		smsService.sendSmsForPhoneAuthentication(phone, code);

		saveAuthCodeInRedis(phone, code);
	}

	private void saveAuthCodeInRedis(String phone, int code) {

		ListOperations<String, String> opsForList = redisTemplate.opsForList();

		String key = AUTH_KEY_PREFIX + phone;

		opsForList.rightPush(key, String.valueOf(code));
		opsForList.rightPush(key, String.valueOf(false));
		redisTemplate.expire(key, 1, TimeUnit.MINUTES);
	}
}
