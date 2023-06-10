package com.dateplan.dateplan.service.couple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
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
			Member member1 = createMember(phone1);
			Member member2 = createMember(phone2);
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
			connectedMember1 = createMember("01012345678");
			connectedMember2 = createMember("01012345679");
			notConnectedMember = createMember("01012345670");
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

	private Member createMember(String phone) {

		return Member.builder()
			.name("name")
			.nickname("nickname")
			.phone(phone)
			.password("password")
			.gender(Gender.FEMALE)
			.build();
	}

}
