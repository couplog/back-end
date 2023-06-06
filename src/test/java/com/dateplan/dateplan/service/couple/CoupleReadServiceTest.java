package com.dateplan.dateplan.service.couple;

import static org.assertj.core.api.Assertions.assertThat;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
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
			Member member1 = createMember(phone1, "password");
			Member member2 = createMember(phone2, "password");
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

	private Member createMember(String phone, String password) {

		return Member.builder()
			.name("name")
			.nickname("nickname")
			.phone(phone)
			.password(password)
			.gender(Gender.FEMALE)
			.build();
	}

}
