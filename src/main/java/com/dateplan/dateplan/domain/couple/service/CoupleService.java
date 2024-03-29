package com.dateplan.dateplan.domain.couple.service;

import static com.dateplan.dateplan.global.util.RandomCodeGenerator.generateConnectionCode;

import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryPatternRepository;
import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryQueryRepository;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.dto.request.FirstDateServiceRequest;
import com.dateplan.dateplan.domain.couple.service.dto.response.FirstDateServiceResponse;
import com.dateplan.dateplan.domain.dating.repository.DatingQueryRepository;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.domain.member.service.dto.request.ConnectionServiceRequest;
import com.dateplan.dateplan.domain.member.service.dto.response.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.service.dto.response.CoupleConnectServiceResponse;
import com.dateplan.dateplan.domain.schedule.repository.SchedulePatternRepository;
import com.dateplan.dateplan.domain.schedule.repository.ScheduleQueryRepository;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
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
	private final SchedulePatternRepository schedulePatternRepository;
	private final AnniversaryPatternRepository anniversaryPatternRepository;
	private final ScheduleQueryRepository scheduleQueryRepository;
	private final AnniversaryQueryRepository anniversaryQueryRepository;
	private final DatingQueryRepository datingQueryRepository;

	public void disconnectCouple(Member member, Long memberId) {
		if (!isSameMember(member.getId(), memberId)) {
			throw new NoPermissionException(Resource.MEMBER, Operation.DELETE);
		}

		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);
		Long partnerId = couple.getPartnerId(member);

		deleteSchedules(memberId, partnerId);
		deleteDating(couple);
		deleteAnniversaries(couple);
		coupleRepository.deleteById(couple.getId());
	}

	private void deleteDating(Couple couple) {
		datingQueryRepository.deleteByCoupleId(couple.getId());
	}

	private void deleteAnniversaries(Couple couple) {
		anniversaryQueryRepository.deleteByCoupleId(couple.getId());
		anniversaryPatternRepository.deleteAllByCoupleId(couple.getId());
	}

	private void deleteSchedules(Long memberId, Long partnerId) {
		scheduleQueryRepository.deleteByMemberId(memberId);
		scheduleQueryRepository.deleteByMemberId(partnerId);

		schedulePatternRepository.deleteAllByMemberId(memberId);
		schedulePatternRepository.deleteAllByMemberId(partnerId);
	}

	@Transactional(readOnly = true)
	public FirstDateServiceResponse getFirstDate(Long coupleId) {
		final Member member = MemberThreadLocal.get();

		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		if (!isSameCouple(coupleId, couple.getId())) {
			throw new NoPermissionException(Resource.COUPLE, Operation.READ);
		}

		return FirstDateServiceResponse.builder()
			.firstDate(couple.getFirstDate())
			.build();
	}

	public void updateFirstDate(Member loginMember, Long coupleId,
		FirstDateServiceRequest request) {

		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(loginMember);

		if (!isSameCouple(coupleId, couple.getId())) {
			throw new NoPermissionException(Resource.COUPLE, Operation.UPDATE);
		}

		couple.updateFirstDate(request.getFirstDate());
	}

	public ConnectionServiceResponse getConnectionCode(Member loginMember, Long memberId) {

		if (!isSameMember(memberId, loginMember.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.READ);
		}
		ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();

		String key = getConnectionKey(loginMember.getId());
		String connectionCode = stringValueOperations.get(key);

		if (connectionCode == null) {
			connectionCode = generateConnectionCode(6);
			while (stringValueOperations.get(connectionCode) != null) {
				connectionCode = generateConnectionCode(6);
			}

			stringValueOperations.set(key, connectionCode);
			stringValueOperations.set(connectionCode, String.valueOf(loginMember.getId()));

			stringValueOperations.getAndExpire(key, Duration.ofHours(24L));
			stringValueOperations.getAndExpire(connectionCode, Duration.ofHours(24L));
		}

		return ConnectionServiceResponse.builder()
			.connectionCode(connectionCode)
			.build();
	}

	public CoupleConnectServiceResponse connectCouple(Member loginMember, Long memberId,
		ConnectionServiceRequest request) {

		if (!isSameMember(memberId, loginMember.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.UPDATE);
		}
		String connectionCode = request.getConnectionCode();
		Long partnerId = getIdOrThrowIfConnectionCodeInvalid(connectionCode);

		Member partner = memberReadService.findMemberByIdOrElseThrow(partnerId);
		throwIfAlreadyConnected(partner);
		throwIfSelfConnection(partnerId, loginMember.getId());

		Couple couple = Couple.builder()
			.member1(loginMember)
			.member2(partner)
			.firstDate(request.getFirstDate())
			.build();
		coupleRepository.save(couple);
		deleteConnectionKey(memberId);
		deleteConnectionKey(partnerId);

		return CoupleConnectServiceResponse.from(couple);
	}

	private void deleteConnectionKey(Long memberId) {
		ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
		String key = stringValueOperations.getAndDelete(getConnectionKey(memberId));

		if (key != null) {
			stringValueOperations.getAndDelete(key);
		}
	}

	private void throwIfAlreadyConnected(Member partner) {
		if (coupleReadService.isMemberConnected(partner)) {
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

	private boolean isSameMember(Long memberId, Long loginMemberId) {

		return Objects.equals(memberId, loginMemberId);
	}

	private boolean isSameCouple(Long coupleId, Long connectedCoupleId) {
		return Objects.equals(coupleId, connectedCoupleId);
	}
}
