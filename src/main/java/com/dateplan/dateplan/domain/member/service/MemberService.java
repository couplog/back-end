package com.dateplan.dateplan.domain.member.service;

import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.dto.SignUpServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final MemberReadService memberReadService;
	private final AuthService authService;
	private final StringRedisTemplate redisTemplate;
	private static final String CONNECTION_PREFIX = "[CONNECTION]";

	public void signUp(SignUpServiceRequest request) {

		String phone = request.getPhone();
		String nickname = request.getNickname();

		memberReadService.throwIfPhoneExists(phone);
		memberReadService.throwIfNicknameExists(nickname);

		authService.throwIfPhoneNotAuthenticated(phone);

		Member member = request.toMember();
		memberRepository.save(member);

		authService.deleteAuthenticationInfoInRedis(phone);
	}

	public ConnectionServiceResponse readConnection() {
		Member member = MemberThreadLocal.get();

		String key = getConnectionKey(member.getId());
		String connectionCode = RandomCodeGenerator.generateConnectionCode(6);

		ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
		stringValueOperations.set(key, connectionCode);
		stringValueOperations.getAndExpire(key, Duration.ofHours(24L));
			return ConnectionServiceResponse.builder()
			.connectionCode(connectionCode)
			.build();
	}

	private String getConnectionKey(Long id) {
		return CONNECTION_PREFIX + id;
	}
}
