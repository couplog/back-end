package com.dateplan.dateplan.domain.couple.service;

import static com.dateplan.dateplan.global.util.RandomCodeGenerator.generateConnectionCode;

import com.dateplan.dateplan.domain.couple.dto.FirstDateServiceRequest;
import com.dateplan.dateplan.domain.couple.dto.FirstDateServiceResponse;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
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

@RequiredArgsConstructor
@Service
@Transactional
public class CoupleService {

	private static final String CONNECTION_PREFIX = "[CONNECTION]";

	private final MemberReadService memberReadService;
	private final StringRedisTemplate redisTemplate;
	private final CoupleRepository coupleRepository;
	private final CoupleReadService coupleReadService;

	public FirstDateServiceResponse getFirstDate() {
		final Member member = MemberThreadLocal.get();

		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		return FirstDateServiceResponse.builder()
			.firstDate(couple.getFirstDate())
			.build();
	}

	public void updateFirstDate(FirstDateServiceRequest request) {
		final Member member = MemberThreadLocal.get();

		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);
		couple.updateFirstDate(request.getFirstDate());
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
		String connectionCode = request.getConnectionCode();
		Long oppositeMemberId = getIdOrThrowIfConnectionCodeInvalid(connectionCode);

		Member oppositeMember = memberReadService.findMemberByIdOrElseThrow(oppositeMemberId);
		throwIfAlreadyConnected(oppositeMember);
		throwIfSelfConnection(oppositeMemberId, member.getId());

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

	private Long getIdOrThrowIfConnectionCodeInvalid(String connectionCode) {
		ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
		String id = stringValueOperations.get(connectionCode);
		if (id == null) {
			throw new InvalidConnectionCodeException();
		}
		return Long.valueOf(id);
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
