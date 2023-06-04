package com.dateplan.dateplan.domain.member.service;

import static com.dateplan.dateplan.global.util.RandomCodeGenerator.generateConnectionCode;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.dto.SignUpServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.exception.member.AlreadyConnectedException;
import com.dateplan.dateplan.global.exception.member.InvalidConnectionCodeException;
import com.dateplan.dateplan.global.exception.member.SelfConnectionNotAllowedException;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

	private static final String CONNECTION_PREFIX = "[CONNECTION]";

	private final MemberRepository memberRepository;
	private final MemberReadService memberReadService;
	private final AuthService authService;
	private final StringRedisTemplate redisTemplate;
	private final CoupleRepository coupleRepository;
	private final CoupleReadService coupleReadService;

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

	public ConnectionServiceResponse getConnectionCode() {
		final Member member = MemberThreadLocal.get();
		ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();

		String key = getConnectionKey(member.getId());
		String connectionCode = stringValueOperations.get(key);

		if (connectionCode == null) {
			connectionCode = generateConnectionCode(6);
			while (stringValueOperations.get(connectionCode) != null) {
				connectionCode = generateConnectionCode(6);
			}

			stringValueOperations.set(key, connectionCode);
			stringValueOperations.set(connectionCode, String.valueOf(member.getId()));

			stringValueOperations.getAndExpire(key, Duration.ofHours(24L));
			stringValueOperations.getAndExpire(connectionCode, Duration.ofHours(24L));
		}

		return ConnectionServiceResponse.builder()
			.connectionCode(connectionCode)
			.build();
	}

	public void connectCouple(ConnectionServiceRequest request) {
		final Member member = MemberThreadLocal.get();
		ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
		String connectionCode = request.getConnectionCode();
		String oppositeMemberId = stringValueOperations.get(connectionCode);

		throwIfInvalidConnectionCode(oppositeMemberId);
		Member oppositeMember = memberReadService.findMemberByIdOrElseThrow(
			Long.valueOf(oppositeMemberId));
		throwIfAlreadyConnected(oppositeMember);
		throwIfSelfConnection(Long.valueOf(oppositeMemberId), member.getId());

		Couple couple = Couple.builder()
			.member1(member)
			.member2(oppositeMember)
			.firstDate(request.getFirstDate())
			.build();
		coupleRepository.save(couple);
	}

	private void throwIfAlreadyConnected(Member oppositeMember) {
		if (coupleReadService.isMemberConnected(oppositeMember)) {
			throw new AlreadyConnectedException();
		}
	}

	private void throwIfInvalidConnectionCode(String oppositeMemberId) {
		if (oppositeMemberId == null) {
			throw new InvalidConnectionCodeException();
		}
	}

	private void throwIfSelfConnection(Long valueOf, Long id) {
		if (Objects.equals(valueOf, id)) {
			throw new SelfConnectionNotAllowedException();
		}
	}

	private String getConnectionKey(Long id) {
		return CONNECTION_PREFIX + id;
	}
}
