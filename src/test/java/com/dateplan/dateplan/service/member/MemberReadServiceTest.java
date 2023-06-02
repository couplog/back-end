package com.dateplan.dateplan.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredNicknameException;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemberReadServiceTest extends ServiceTestSupport {

	@Autowired
	private MemberReadService memberReadService;

	@Autowired
	private MemberRepository memberRepository;

	@AfterEach
	void tearDown() {
		memberRepository.deleteAllInBatch();
	}

	@DisplayName("전화번호가 주어졌을 때")
	@Nested
	class ThrowIfPhoneExists {

		private final String existsPhoneNumber = "01011112222";

		@BeforeEach
		void setUp() {
			Member member = createMember(existsPhoneNumber, "nickname");
			memberRepository.save(member);
		}

		@DisplayName("해당 전화번호가 이미 가입되어 있다면 예외를 발생시킨다.")
		@Test
		void throwIfPhoneExists() {

			//When & Then
			assertThatThrownBy(() -> memberReadService.throwIfPhoneExists(existsPhoneNumber))
				.isInstanceOf(AlReadyRegisteredPhoneException.class)
				.hasMessage(DetailMessage.ALREADY_REGISTERED_PHONE);
		}

		@DisplayName("해당 전화번호가 가입되어 있지 않다면 예외를 발생시키지 않는다.")
		@Test
		void notThrowIfPhoneNotExists() {

			// Given
			String notExistsPhoneNumber = "01012341234";

			//When & Then
			assertThatNoException().isThrownBy(
				() -> memberReadService.throwIfPhoneExists(notExistsPhoneNumber));
		}
	}

	@DisplayName("전화번호가 주어졌을 때")
	@Nested
	class FindMemberByPhoneOrElseThrow {

		private final String existsPhoneNumber = "01011112222";

		@BeforeEach
		void setUp() {
			Member member = createMember(existsPhoneNumber, "nickname");
			memberRepository.save(member);
		}

		@DisplayName("해당 번호로 가입된 회원이 있다면 회원 엔티티를 반환한다.")
		@Test
		void IfExists() {

			// When & Then
			Member findMember = memberReadService.findMemberByPhoneOrElseThrow(
				existsPhoneNumber);

			assertThat(findMember.getPhone()).isEqualTo(existsPhoneNumber);
		}

		@DisplayName("해당 번호로 가입된 회원이 없다면 예외를 발생시킨다.")
		@Test
		void IfNotExists() {

			// Given
			String notExistsPhoneNumber = "01012341234";

			// When & Then
			assertThatThrownBy(() -> memberReadService.findMemberByPhoneOrElseThrow(
				notExistsPhoneNumber))
				.isInstanceOf(MemberNotFoundException.class)
				.hasMessage(DetailMessage.MEMBER_NOT_FOUND);
		}
	}

	@DisplayName("닉네임이 주어졌을 때")
	@Nested
	class ThrowIfNicknameExists {

		private final String existsNickname = "nickname";

		@BeforeEach
		void setUp(){
			Member member = createMember("01011112222", existsNickname);
			memberRepository.save(member);
		}

		@DisplayName("해당 닉네임이 이미 가입되어 있다면 예외를 발생시킨다.")
		@Test
		void throwIfNicknameExists() {

			//When & Then
			assertThatThrownBy(() -> memberReadService.throwIfNicknameExists(existsNickname))
				.isInstanceOf(AlReadyRegisteredNicknameException.class)
				.hasMessage(DetailMessage.ALREADY_REGISTERED_NICKNAME);
		}

		@DisplayName("해당 닉네임이 가입되어 있지 않다면 예외를 발생시키지 않는다.")
		@Test
		void notThrowIfNicknameNotExists() {

			// Given
			String notExistsNickname = "newNickname";

			//When & Then
			assertThatNoException().isThrownBy(
				() -> memberReadService.throwIfPhoneExists(notExistsNickname));
		}
	}

	private Member createMember(String phone, String nickname) {

		return Member.builder()
			.name("name")
			.nickname(nickname)
			.phone(phone)
			.password("password")
			.gender(Gender.FEMALE)
			.profileImageUrl("url")
			.build();
	}
}
