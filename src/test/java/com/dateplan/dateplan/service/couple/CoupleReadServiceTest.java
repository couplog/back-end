package com.dateplan.dateplan.service.couple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dateplan.dateplan.domain.couple.dto.CoupleInfoServiceResponse;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.couple.CoupleNotFoundException;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CoupleReadServiceTest extends ServiceTestSupport {

	@Autowired
	private CoupleReadService coupleReadService;

	@Autowired
	private CoupleRepository coupleRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberReadService memberReadService;

	@Nested
	@DisplayName("멤버가 주어졌을 때")
	class findMemberConnected {

		String phone1 = "01012345678";
		String phone2 = "01012345679";

		@AfterEach
		void tearDown() {
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@BeforeEach
		void setUp() {
			Member member1 = createMember(phone1, "nickname1");
			Member member2 = createMember(phone2, "nickname2");
			memberRepository.save(member1);
			memberRepository.save(member2);
		}

		@DisplayName("커플 연결되었을 때 커플 여부를 확인한다")
		@Test
		void checkCoupleConnected() {

			// Given
			Member member1 = memberReadService.findMemberByPhoneOrElseThrow(phone1);
			Member member2 = memberReadService.findMemberByPhoneOrElseThrow(phone2);

			// When
			Couple couple = Couple.builder()
				.member1(member1)
				.member2(member2)
				.firstDate(LocalDate.now().minusDays(1L))
				.build();
			coupleRepository.save(couple);

			// Then
			assertThat(coupleReadService.isMemberConnected(member1)).isTrue();
			assertThat(coupleReadService.isMemberConnected(member2)).isTrue();
		}

		@DisplayName("커플 연결 되지 않았을 때 커플 여부를 확인한다")
		@Test
		void checkCoupleDisconnected() {

			// Given
			Member member1 = memberReadService.findMemberByPhoneOrElseThrow(phone1);
			Member member2 = memberReadService.findMemberByPhoneOrElseThrow(phone2);

			// When & Then
			assertThat(coupleReadService.isMemberConnected(member1)).isFalse();
			assertThat(coupleReadService.isMemberConnected(member2)).isFalse();
		}
	}

	@Nested
	@DisplayName("멤버로 연결되어 있는 커플을 찾을 때")
	class FindCoupleByMember {

		Member connectedMember1;
		Member connectedMember2;
		Member notConnectedMember;

		@BeforeEach
		void setUp() {
			connectedMember1 = createMember("01012345678", "nickname1");
			connectedMember2 = createMember("01012345679", "nickname2");
			notConnectedMember = createMember("01012345670", "nickname3");
			memberRepository.saveAll(
				List.of(connectedMember1, connectedMember2, notConnectedMember));
			Couple couple = Couple.builder()
				.member1(connectedMember1)
				.member2(connectedMember2)
				.firstDate(LocalDate.of(2010, 10, 10))
				.build();
			coupleRepository.save(couple);
		}

		@AfterEach
		void tearDown() {
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("커플이 연결되어 있는 멤버를 입력하면 올바른 커플을 반환한다")
		@Test
		void returnCoupleWithConnectedMember() {

			Couple couple1 = coupleReadService.findCoupleByMemberOrElseThrow(connectedMember1);
			Couple couple2 = coupleReadService.findCoupleByMemberOrElseThrow(connectedMember2);

			assertThat(couple1.getId()).isEqualTo(couple2.getId());
		}

		@DisplayName("커플이 연결되어 있지 않은 멤버를 입력하면 실패한다")
		@Test
		void failWithNotConnectedMember() {

			assertThatThrownBy(
				() -> coupleReadService.findCoupleByMemberOrElseThrow(notConnectedMember))
				.isInstanceOf(MemberNotConnectedException.class)
				.hasMessage(DetailMessage.Member_NOT_CONNECTED);
		}
	}

	@Nested
	@DisplayName("현재 연결되어 있는 커플 정보를 조회하려 할 때")
	class getCoupleInfo {

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

		@DisplayName("요청한 회원이 커플에 연결되어 있다면 성공한다")
		@Test
		void successWithConnected() {

			// When
			CoupleInfoServiceResponse response = coupleReadService.getCoupleInfo();

			// Then
			assertThat(response.getCoupleId()).isEqualTo(couple.getId());
			assertThat(response.getFirstDate()).isEqualTo(couple.getFirstDate());
			assertThat(response.getPartnerId()).isEqualTo(member2.getId());
		}

		@DisplayName("요청한 회원이 커플에 연결되어 있지 않다면 실패한다")
		@Test
		void failWithNotConnected() {

			// Given
			Member notConnectedMember = memberRepository.save(
				createMember("01011112222", "nickname3"));
			MemberThreadLocal.set(notConnectedMember);

			// When & Then
			assertThatThrownBy(() -> coupleReadService.getCoupleInfo())
				.isInstanceOf(MemberNotConnectedException.class)
				.hasMessage(DetailMessage.Member_NOT_CONNECTED);
		}
	}

	@Nested
	@DisplayName("주어진 회원과 연결된 회원의 id를 조회할 때")
	class getPartnerId {

		Member connectedMember1;
		Member connectedMember2;
		Member notConnectedMember;

		@BeforeEach
		void setUp() {
			connectedMember1 = createMember("01012345678", "nickname1");
			connectedMember2 = createMember("01012345679", "nickname2");
			notConnectedMember = createMember("01012345555", "nickname3");
			memberRepository.saveAll(
				List.of(connectedMember1, connectedMember2, notConnectedMember));
			Couple couple = createCouple(connectedMember1, connectedMember2);
			coupleRepository.save(couple);
		}

		@AfterEach
		void tearDown() {
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("회원이 다른 회원과 연결되어 있다면 상대방 id 를 응답한다.")
		@Test
		void withConnectedMember() {

			// When
			Long partnerId1 = coupleReadService.getPartnerId(connectedMember1);
			Long partnerId2 = coupleReadService.getPartnerId(connectedMember2);

			// Then
			assertThat(partnerId1).isEqualTo(connectedMember2.getId());
			assertThat(partnerId2).isEqualTo(connectedMember1.getId());
		}

		@DisplayName("회원이 다른 회원과 연결되어 있지 않다면 실패한다.")
		@Test
		void withNotConnectedMember() {

			// When & Then
			assertThatThrownBy(() -> coupleReadService.getPartnerId(notConnectedMember))
				.isInstanceOf(MemberNotConnectedException.class)
				.hasMessage(DetailMessage.Member_NOT_CONNECTED);
		}
	}

	@Nested
	@DisplayName("커플 id가 주어졌을 때")
	class FindCoupleByIdOrElseThrow {

		private String phone1 = "01012345678";
		private String phone2 = "01012345679";
		private Couple couple;

		@BeforeEach
		void setUp() {
			Member member1 = createMember(phone1, "nickname1");
			Member member2 = createMember(phone2, "nickname2");
			memberRepository.save(member1);
			memberRepository.save(member2);

			couple = createCouple(member1, member2);
			coupleRepository.save(couple);
		}

		@AfterEach
		void tearDown() {
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("존재하는 커플 id 라면 커플 엔티티를 반환한다.")
		@Test
		void withExistsCoupleId() {

			// Given
			Long coupleId = couple.getId();

			// When
			Couple findCouple = coupleReadService.findCoupleByIdOrElseThrow(coupleId);

			// Then
			assertThat(findCouple.getId())
				.isEqualTo(couple.getId());
		}

		@DisplayName("존재하지 않는 커플 id 라면 예외를 발생시킨다.")
		@Test
		void withNotExistsCoupleId() {

			// Given
			Long coupleId = couple.getId() + 100;

			// When & Then
			assertThatThrownBy(() -> coupleReadService.findCoupleByIdOrElseThrow(coupleId))
				.isInstanceOf(CoupleNotFoundException.class)
				.hasMessage(DetailMessage.COUPLE_NOT_FOUND);
		}
	}

	private Member createMember(String phone, String nickname) {

		return Member.builder()
			.name("name")
			.nickname(nickname)
			.phone(phone)
			.password("password")
			.gender(Gender.FEMALE)
			.birthDay(LocalDate.of(2000, 10, 10))
			.build();
	}

	private Couple createCouple(Member member1, Member member2) {

		return Couple.builder()
			.member1(member1)
			.member2(member2)
			.firstDate(LocalDate.of(2010, 10, 10))
			.build();
	}

}
