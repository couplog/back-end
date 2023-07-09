package com.dateplan.dateplan.service.dating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.domain.dating.repository.DatingRepository;
import com.dateplan.dateplan.domain.dating.service.DatingReadService;
import com.dateplan.dateplan.domain.dating.service.DatingService;
import com.dateplan.dateplan.domain.dating.service.dto.request.DatingCreateServiceRequest;
import com.dateplan.dateplan.domain.dating.service.dto.request.DatingUpdateServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.global.exception.dating.DatingNotFoundException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

public class DatingServiceTest extends ServiceTestSupport {

	@Autowired
	private CoupleRepository coupleRepository;

	@Autowired
	private DatingRepository datingRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private DatingService datingService;

	@SpyBean
	private DatingReadService datingReadService;

	@Nested
	@DisplayName("데이트 일정 생성 시")
	class CreateDating {

		private static final String NEED_COUPLE = "needDating";

		Member member;
		Couple couple;

		@BeforeEach
		void setUp(TestInfo testInfo) {
			member = memberRepository.save(createMember("01012345678", "nickname1"));
			MemberThreadLocal.set(member);

			if (testInfo.getTags().contains(NEED_COUPLE)) {
				Member partner = memberRepository.save(createMember("01012345679", "nickname2"));

				couple = coupleRepository.save(Couple.builder()
					.member1(member)
					.member2(partner)
					.firstDate(LocalDate.now())
					.build()
				);
			}
		}

		@AfterEach
		void tearDown(TestInfo testInfo) {
			MemberThreadLocal.remove();

			if (testInfo.getTags().contains(NEED_COUPLE)) {
				datingRepository.deleteAllInBatch();
				coupleRepository.deleteAllInBatch();
			}
			memberRepository.deleteAllInBatch();
		}

		@Tag(NEED_COUPLE)
		@DisplayName("올바른 coupleId, requestDTO를 요청하면 데이트 일정이 생성된다")
		@Test
		void successWithValidRequest() {

			// Given
			DatingCreateServiceRequest request = createServiceRequest();

			// When
			datingService.createDating(member, couple.getId(), request);

			// Then
			List<Dating> datingList = datingRepository.findAll();
			Dating dating = datingList.get(0);

			assertThat(dating.getTitle()).isEqualTo(request.getTitle());
			assertThat(dating.getContent()).isEqualTo(request.getContent());
			assertThat(dating.getLocation()).isEqualTo(request.getLocation());
			assertThat(dating.getStartDateTime()).isEqualTo(request.getStartDateTime());
			assertThat(dating.getEndDateTime()).isEqualTo(request.getEndDateTime());
		}

		@Tag(NEED_COUPLE)
		@DisplayName("로그인한 회원의 coupleId와 요청의 coupleId가 다르면 실패한다")
		@Test
		void failWithNoPermission() {

			// Given
			DatingCreateServiceRequest request = createServiceRequest();

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.CREATE);
			assertThatThrownBy(
				() -> datingService.createDating(member, couple.getId() + 100, request))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}

		@DisplayName("회원이 연결되어 있지 않으면 실패한다")
		@Test
		void failWithMemberNotConnected() {

			// Given
			DatingCreateServiceRequest request = createServiceRequest();

			// When & Then
			MemberNotConnectedException exception = new MemberNotConnectedException();
			assertThatThrownBy(() -> datingService.createDating(member, 1L, request))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	@Nested
	@DisplayName("데이트 일정 삭제 시")
	class DeleteDating {

		private Member member;
		private Couple couple;
		private Dating savedDating;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("01012345678", "aaa"));
			Member partner = memberRepository.save(createMember("01012345679", "bbb"));

			couple = coupleRepository.save(Couple.builder()
				.member1(member)
				.member2(partner)
				.firstDate(LocalDate.now())
				.build());

			savedDating = datingRepository.save(Dating.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().plusHours(1))
				.couple(couple)
				.build()
			);
		}

		@AfterEach
		void tearDown() {
			datingRepository.deleteAllInBatch();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Test
		void 성공_올바른coupleId와_존재하는데이트일정의Id를요청하면_해당데이트일정이삭제된다() {

			// When
			datingService.deleteDating(member, couple.getId(), savedDating.getId());

			// When
			assertThat(datingRepository.findById(savedDating.getId())).isEmpty();
		}

		@Test
		void 실패_요청한coupldId와_회원이연결된커플의id가다르면_예외를반환한다() {

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.DELETE);
			assertThatThrownBy(() ->
				datingService.deleteDating(member, couple.getId() + 100, savedDating.getId()))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());

			// Verify
			then(datingReadService)
				.shouldHaveNoInteractions();
		}

		@Test
		void 실패_회원이_연결되어있지않으면_예외를반환한다() {

			// Given
			Member other = memberRepository.save(createMember("01011112222", "ccc"));

			// When & Then
			MemberNotConnectedException exception = new MemberNotConnectedException();
			assertThatThrownBy(
				() -> datingService.deleteDating(other, couple.getId(), savedDating.getId()))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());

			// Verify
			then(datingReadService)
				.shouldHaveNoInteractions();
		}

		@Test
		void 실패_존재하지않는_데이트일정id를요청하면_예외를반환한다() {

			// When & Then
			DatingNotFoundException exception = new DatingNotFoundException();
			assertThatThrownBy(() ->
				datingService.deleteDating(member, couple.getId(), savedDating.getId() + 100))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	@Nested
	@DisplayName("데이트 일정 수정 시")
	class UpdateDating {

		private Member member;
		private Couple couple;
		private Dating savedDating;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("01012345678", "aaa"));
			Member partner = memberRepository.save(createMember("01012345679", "bbb"));

			couple = coupleRepository.save(Couple.builder()
				.member1(member)
				.member2(partner)
				.firstDate(LocalDate.now())
				.build()
			);
			savedDating = datingRepository.save(Dating.builder()
				.title("title")
				.location("location")
				.content("content")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().plusHours(1))
				.couple(couple)
				.build()
			);
		}

		@AfterEach
		void tearDown() {
			datingRepository.deleteAllInBatch();
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@Test
		void 성공_올바른coupleId와datingId와DTO를입력하면_데이트일정이수정된다() {

			// Given
			DatingUpdateServiceRequest request = createDatingUpdateServiceRequest();

			// When
			datingService.updateDating(member, couple.getId(), savedDating.getId(), request);

			// Then
			Dating updatedDating = datingRepository.findById(savedDating.getId()).get();
			assertThat(updatedDating.getTitle()).isEqualTo(request.getTitle());
			assertThat(updatedDating.getLocation()).isEqualTo(request.getLocation());
			assertThat(updatedDating.getContent()).isEqualTo(request.getContent());
			assertThat(updatedDating.getStartDateTime()).isEqualTo(request.getStartDateTime());
			assertThat(updatedDating.getEndDateTime()).isEqualTo(request.getEndDateTime());
		}

		@Test
		void 실패_회원이_연결되어있지않으면_예외를반환한다() {

			// Given
			DatingUpdateServiceRequest request = createDatingUpdateServiceRequest();
			Member other = memberRepository.save(createMember("01011112222", "ccc"));

			// When & Then
			MemberNotConnectedException exception = new MemberNotConnectedException();
			assertThatThrownBy(() -> datingService.updateDating(other, couple.getId(),
				savedDating.getId(), request))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());

			then(datingReadService)
				.shouldHaveNoInteractions();
		}

		@Test
		void 실패_요청한coupleId와_연결된커플의Id가다르면_예외를반환한다() {

			// Given
			DatingUpdateServiceRequest request = createDatingUpdateServiceRequest();

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.COUPLE,
				Operation.UPDATE);
			assertThatThrownBy(() -> datingService.updateDating(member, couple.getId() + 100,
				savedDating.getId(), request))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());

			then(datingReadService)
				.shouldHaveNoInteractions();
		}

		@Test
		void 실패_요청에해당하는_데이트일정이존재하지않으면_예외를반환한다() {

			// Given
			DatingUpdateServiceRequest request = createDatingUpdateServiceRequest();

			// When & Then
			DatingNotFoundException exception = new DatingNotFoundException();
			assertThatThrownBy(() -> datingService.updateDating(member, couple.getId(),
				savedDating.getId() + 100, request))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	private DatingUpdateServiceRequest createDatingUpdateServiceRequest() {
		LocalDateTime newStartDateTime = LocalDateTime.now().plusDays(5)
			.truncatedTo(ChronoUnit.SECONDS);
		LocalDateTime newEndDateTime = LocalDateTime.now().plusDays(5).plusHours(1)
			.truncatedTo(ChronoUnit.SECONDS);

		return DatingUpdateServiceRequest.builder()
			.title("newTitle")
			.location("newLocation")
			.content("newContent")
			.startDateTime(newStartDateTime)
			.endDateTime(newEndDateTime)
			.build();
	}

	private Member createMember(String phone, String nickname) {
		return Member.builder()
			.name("name")
			.phone(phone)
			.password("password")
			.nickname(nickname)
			.gender(Gender.MALE)
			.birthDay(LocalDate.of(2010, 10, 10))
			.build();
	}

	private DatingCreateServiceRequest createServiceRequest() {
		return DatingCreateServiceRequest.builder()
			.title("title")
			.content("content")
			.location("location")
			.startDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
			.endDateTime(LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.MINUTES))
			.build();
	}
}
