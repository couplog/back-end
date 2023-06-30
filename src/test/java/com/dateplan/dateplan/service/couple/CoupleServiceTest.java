package com.dateplan.dateplan.service.couple;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_CONNECTED;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CONNECTION_CODE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.SELF_CONNECTION_NOT_ALLOWED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.dateplan.dateplan.domain.couple.service.dto.request.FirstDateServiceRequest;
import com.dateplan.dateplan.domain.couple.service.dto.response.FirstDateServiceResponse;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.domain.member.service.dto.request.ConnectionServiceRequest;
import com.dateplan.dateplan.domain.member.service.dto.response.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.service.dto.response.CoupleConnectServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.global.exception.member.AlreadyConnectedException;
import com.dateplan.dateplan.global.exception.member.InvalidConnectionCodeException;
import com.dateplan.dateplan.global.exception.member.SelfConnectionNotAllowedException;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class CoupleServiceTest extends ServiceTestSupport {

	@Autowired
	private CoupleService coupleService;

	@SpyBean
	private StringRedisTemplate redisTemplate;

	@SpyBean
	private CoupleRepository coupleRepository;

	@Autowired
	private MemberRepository memberRepository;

	@MockBean
	private MemberReadService memberReadService;

	@DisplayName("연결 코드 조회 시")
	@Nested
	class GetConnectionCode {

		Member member;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("01012345678", "nickname"));
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
			MemberThreadLocal.remove();
		}

		@DisplayName("자신의 id가 아닌 다른 id를 요청하면 실패한다.")
		@Test
		void failWithoutPermission() {

			// Given
			Long id = member.getId() + 1;

			// When & Then
			assertThatThrownBy(() -> coupleService.getConnectionCode(id))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(String.format(DetailMessage.NO_PERMISSION, Resource.MEMBER.getName(),
					Operation.READ.getName()));

			// Verify
			then(redisTemplate)
				.shouldHaveNoInteractions();
			try (MockedStatic<RandomCodeGenerator> generator = mockStatic(
				RandomCodeGenerator.class)) {

				generator.verify(() -> RandomCodeGenerator.generateConnectionCode(anyInt()),
					never());
			}
		}

		@DisplayName("24시간 내에 생성된 코드가 없다면 새로 생성한 코드를 반환하고, redis에 저장된다")
		@Test
		void returnNewConnectionCode() {

			// Given
			String connectionCode = "ABC123";
			ConnectionServiceResponse response;

			try (MockedStatic<RandomCodeGenerator> generator = mockStatic(
				RandomCodeGenerator.class)) {

				// Stub
				given(RandomCodeGenerator.generateConnectionCode(6)).willReturn(connectionCode);

				// When
				response = coupleService.getConnectionCode(member.getId());

				// Verify
				generator.verify(() -> RandomCodeGenerator.generateConnectionCode(anyInt()),
					times(1));
			}

			ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

			String savedCode = opsForValue.get(getConnectionKey(member.getId()));
			String savedId = opsForValue.get(connectionCode);

			// Then
			assertThat(savedCode).isEqualTo(connectionCode);
			assertThat(savedId).isEqualTo(String.valueOf(member.getId()));
			assertThat(response.getConnectionCode()).isEqualTo(connectionCode);
		}

		@DisplayName("24시간 내에 생성된 코드가 있다면, 이미 생성된 코드를 반환한다")
		@Test
		void returnPreCreatedConnectionCode() {

			// Given
			String connectionCode = "ABC123";
			ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
			String key = getConnectionKey(member.getId());
			valueOperations.set(key, connectionCode);
			ConnectionServiceResponse savedConnectionCode;

			try (MockedStatic<RandomCodeGenerator> generator = mockStatic(
				RandomCodeGenerator.class)) {

				// Stubbing
				generator.when(
						() -> RandomCodeGenerator.generateConnectionCode(anyInt()))
					.thenAnswer(invocation -> null);

				// When
				savedConnectionCode = coupleService.getConnectionCode(member.getId());

				// Verify
				generator.verify(
					() -> RandomCodeGenerator.generateConnectionCode(anyInt()), never());
			}

			// Then
			assertThat(savedConnectionCode.getConnectionCode()).isEqualTo(connectionCode);
		}

		@DisplayName("이미 코드가 존재한다면, 반복된 코드가 나오지 않을때까지 생성한다.")
		@Test
		void returnNotDuplicatedConnectionCode() {

			// Given
			ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

			String savedConnectionCode = "ABC123";
			String newConnectionCode = "123ABC";
			valueOperations.set(savedConnectionCode, "someId");

			ConnectionServiceResponse response;

			try (MockedStatic<RandomCodeGenerator> generator = mockStatic(
				RandomCodeGenerator.class)) {

				// Stubbing
				given(RandomCodeGenerator.generateConnectionCode(6))
					.willReturn(savedConnectionCode)
					.willReturn(savedConnectionCode)
					.willReturn(newConnectionCode);

				// When
				response = coupleService.getConnectionCode(member.getId());

				// Verify
				generator.verify(
					() -> RandomCodeGenerator.generateConnectionCode(anyInt()), times(3));
			}

			// Then
			assertThat(response.getConnectionCode()).isEqualTo(newConnectionCode);
		}
	}

	@Nested
	@DisplayName("회원 연결 시")
	class ConnectCouple {

		Member member;
		Member partner;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("01012345678", "nickname1"));
			partner = memberRepository.save(createMember("01012345679", "nickname2"));
			MemberThreadLocal.set(member);
		}

		@AfterEach
		void tearDown() {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
			MemberThreadLocal.remove();
		}

		@DisplayName("자신의 id가 아닌 다른 id를 요청하면 실패한다.")
		@Test
		void failWithoutPermission() {

			// Given
			Long id = member.getId() + 1;
			ConnectionServiceRequest request = createConnectionServiceRequest("ABC123");

			// When & Then
			assertThatThrownBy(() -> coupleService.connectCouple(id, request))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(String.format(DetailMessage.NO_PERMISSION, Resource.MEMBER.getName(),
					Operation.UPDATE.getName()));

			// Verify
			then(redisTemplate)
				.shouldHaveNoInteractions();
			try (MockedStatic<RandomCodeGenerator> generator = mockStatic(
				RandomCodeGenerator.class)) {

				generator.verify(() -> RandomCodeGenerator.generateConnectionCode(anyInt()),
					never());
			}
		}

		@DisplayName("올바른 요청 코드를 입력하면 상대방과 연결되고, 커플 정보를 담아 응답한다.")
		@Test
		void connectCoupleWithValidRequest() {

			// Given
			String connectionCode = "ABC123";

			ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
			opsForValue.set(getConnectionKey(partner.getId()), connectionCode);
			opsForValue.set(connectionCode, String.valueOf(partner.getId()));

			ConnectionServiceRequest request = createConnectionServiceRequest(connectionCode);

			// Stubbing
			given(memberReadService.findMemberByIdOrElseThrow(anyLong()))
				.willReturn(partner);
			given(coupleRepository.findById(anyLong())).willReturn(
				Optional.ofNullable(createCouple(member, partner)));

			// When
			CoupleConnectServiceResponse serviceResponse = coupleService.connectCouple(
				member.getId(), request);

			// Then
			Couple couple = coupleRepository.findById(1L).orElse(null);
			assertThat(couple).isNotNull();
			assertThat(couple.getFirstDate()).isEqualTo(request.getFirstDate());
			assertThat(couple.getMember1().getId()).isIn(member.getId(), partner.getId());
			assertThat(couple.getMember2().getId()).isIn(member.getId(), partner.getId());

			assertThat(serviceResponse).isNotNull();
			assertThat(List.of(serviceResponse.getMember1Id(), serviceResponse.getMember2Id()))
				.containsExactlyInAnyOrder(member.getId(), partner.getId());
		}

		@DisplayName("존재하지 않는 코드를 입력하면 실패한다")
		@Test
		void failWithInvalidRequest() {

			// Given
			String connectionCode = "ABC123";
			ConnectionServiceRequest request = createConnectionServiceRequest(connectionCode);

			// Stubbing
			given(memberReadService.findMemberByIdOrElseThrow(anyLong()))
				.willReturn(partner);

			// When & Then
			assertThatThrownBy(() -> coupleService.connectCouple(member.getId(), request))
				.isInstanceOf(InvalidConnectionCodeException.class)
				.hasMessage(INVALID_CONNECTION_CODE);
		}

		@DisplayName("상대방이 이미 연결된 경우라면 실패한다")
		@Test
		void failWithAlreadyConnected() {

			// Given
			coupleRepository.save(createCouple(member, partner));
			String connectionCode = "ABC123";
			ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
			opsForValue.set(connectionCode, String.valueOf(partner.getId()));
			ConnectionServiceRequest request = createConnectionServiceRequest(connectionCode);

			// Stubbing
			given(memberReadService.findMemberByIdOrElseThrow(anyLong()))
				.willReturn(partner);

			// When & Then
			assertThatThrownBy(() -> coupleService.connectCouple(member.getId(), request))
				.isInstanceOf(AlreadyConnectedException.class)
				.hasMessage(ALREADY_CONNECTED);
		}

		@DisplayName("자기 자신과 연결하려 하면 실패한다")
		@Test
		void failWithSelfConnection() {

			// Given
			String connectionCode = "ABC123";

			ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
			opsForValue.set(getConnectionKey(member.getId()), connectionCode);
			opsForValue.set(connectionCode, String.valueOf(member.getId()));

			ConnectionServiceRequest request = createConnectionServiceRequest(connectionCode);

			// Stubbing
			given(memberReadService.findMemberByIdOrElseThrow(anyLong()))
				.willReturn(partner);

			// When & Then
			assertThatThrownBy(() -> coupleService.connectCouple(member.getId(), request))
				.isInstanceOf(SelfConnectionNotAllowedException.class)
				.hasMessage(SELF_CONNECTION_NOT_ALLOWED);
		}
	}

	@Nested
	@DisplayName("커플 처음 만난 날 조회 시")
	class GetFirstDate {

		Member member1;
		Member member2;
		Couple couple;

		@AfterEach
		void tearDown() {
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
			MemberThreadLocal.remove();
		}

		@BeforeEach
		void setUp() {
			member1 = createMember("01012345678", "nickname1");
			member2 = createMember("01012345679", "nickname2");
			memberRepository.saveAll(List.of(member1, member2));
			couple = coupleRepository.save(createCouple(member1, member2));
			MemberThreadLocal.set(member1);
		}

		@DisplayName("올바른 커플 아이디를 입력하면 성공한다.")
		@Test
		void successWithValidCoupleId() {

			// Given
			Long coupleId = couple.getId();

			// When
			FirstDateServiceResponse firstDate = coupleService.getFirstDate(coupleId);

			// Then
			assertThat(firstDate.getFirstDate())
				.isEqualTo(couple.getFirstDate());
		}

		@DisplayName("연결되지 않은 멤버가 요청하면 실패한다")
		@Test
		void failWithNotConnectedMember() {

			// Given
			Member nowConnectedMember = createMember("01011111111", "nickname");
			MemberThreadLocal.set(nowConnectedMember);
			memberRepository.save(nowConnectedMember);

			// When & Then
			assertThatThrownBy(() -> coupleService.getFirstDate(couple.getId()))
				.isInstanceOf(MemberNotConnectedException.class)
				.hasMessage(DetailMessage.Member_NOT_CONNECTED);
		}

		@DisplayName("현재 자신이 연결된 coupleId와 파라미터의 coupleId가 다르면 실패한다.")
		@Test
		void failWithDifferentCoupleId() {
			// Given
			Long coupleId = couple.getId() + 1;

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.READ);

			assertThatThrownBy(() -> coupleService.getFirstDate(coupleId))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	@Nested
	@DisplayName("커플 처음 만난 날 수정 시")
	class UpdateFirstDate {

		Member member1;
		Member member2;
		Couple couple;

		@AfterEach
		void tearDown() {
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
			MemberThreadLocal.remove();
		}

		@BeforeEach
		void setUp() {
			member1 = createMember("01012345678", "nickname1");
			member2 = createMember("01012345679", "nickname2");
			memberRepository.saveAll(List.of(member1, member2));
			couple = coupleRepository.save(createCouple(member1, member2));
			MemberThreadLocal.set(member1);
		}

		@DisplayName("올바른 커플 아이디를 입력하면 성공한다.")
		@Test
		void successWithValidCoupleId() {

			// Given
			Long coupleId = couple.getId();
			FirstDateServiceRequest request = createFirstDateServiceRequest();

			// When
			coupleService.updateFirstDate(coupleId, request);

			// Then
			Couple updatedCouple = coupleRepository.findById(coupleId).get();
			assertThat(updatedCouple.getFirstDate())
				.isEqualTo(request.getFirstDate());
		}

		@DisplayName("연결되지 않은 멤버가 요청하면 실패한다")
		@Test
		void failWithNotConnectedMember() {

			// Given
			Member nowConnectedMember = createMember("01011111111", "nickname");
			MemberThreadLocal.set(nowConnectedMember);
			memberRepository.save(nowConnectedMember);
			FirstDateServiceRequest request = createFirstDateServiceRequest();

			// When & Then
			assertThatThrownBy(() -> coupleService.updateFirstDate(couple.getId(), request))
				.isInstanceOf(MemberNotConnectedException.class)
				.hasMessage(DetailMessage.Member_NOT_CONNECTED);
		}

		@DisplayName("현재 자신이 연결된 coupleId와 파라미터의 coupleId가 다르면 실패한다.")
		@Test
		void failWithDifferentCoupleId() {
			// Given
			Long coupleId = couple.getId() + 1;
			FirstDateServiceRequest request = createFirstDateServiceRequest();

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.UPDATE);

			assertThatThrownBy(() -> coupleService.updateFirstDate(coupleId, request))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	private FirstDateServiceRequest createFirstDateServiceRequest() {
		return FirstDateServiceRequest.builder()
			.firstDate(LocalDate.of(2020, 10, 10))
			.build();
	}

	private Couple createCouple(Member member, Member partner) {
		return Couple.builder()
			.member1(member)
			.member2(partner)
			.firstDate(LocalDate.now().minusDays(1L))
			.build();
	}

	private Member createMember(String phone, String nickname) {
		return Member.builder()
			.phone(phone)
			.password("password")
			.name("name")
			.birthDay(LocalDate.now().minusDays(1L))
			.gender(Gender.MALE)
			.nickname(nickname)
			.build();
	}

	private String getConnectionKey(Long id) {
		return "[CONNECTION]" + id;
	}

	private ConnectionServiceRequest createConnectionServiceRequest(String connectionCode) {
		return ConnectionServiceRequest.builder()
			.connectionCode(connectionCode)
			.firstDate(LocalDate.now().minusDays(1L))
			.build();
	}
}
