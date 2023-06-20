package com.dateplan.dateplan.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dateplan.dateplan.domain.member.dto.MemberInfoServiceResponse;
import com.dateplan.dateplan.domain.member.dto.ProfileImageURLServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.member.AlReadyRegisteredNicknameException;
import com.dateplan.dateplan.global.exception.member.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import java.util.List;
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

	@DisplayName("전화번호가 주어졌을 때")
	@Nested
	class ThrowIfPhoneExists {

		private final String existsPhoneNumber = "01011112222";

		@BeforeEach
		void setUp() {
			Member member = createMember(existsPhoneNumber, "nickname");
			memberRepository.save(member);
		}

		@AfterEach
		void tearDown() {
			memberRepository.deleteAllInBatch();
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

		@AfterEach
		void tearDown() {
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("해당 번호로 가입된 회원이 있다면 회원 엔티티를 반환한다.")
		@Test
		void IfExists() {

			// When & Then
			Member findMember = memberReadService.findMemberByPhoneOrElseThrow(existsPhoneNumber);

			assertThat(findMember.getPhone()).isEqualTo(existsPhoneNumber);
		}

		@DisplayName("해당 번호로 가입된 회원이 없다면 예외를 발생시킨다.")
		@Test
		void IfNotExists() {

			// Given
			String notExistsPhoneNumber = "01012341234";

			// When & Then
			assertThatThrownBy(
				() -> memberReadService.findMemberByPhoneOrElseThrow(notExistsPhoneNumber))
				.isInstanceOf(MemberNotFoundException.class)
				.hasMessage(DetailMessage.MEMBER_NOT_FOUND);
		}
	}

	@DisplayName("닉네임이 주어졌을 때")
	@Nested
	class ThrowIfNicknameExists {

		private final String existsNickname = "nickname";

		@BeforeEach
		void setUp() {
			Member member = createMember("01011112222", existsNickname);
			memberRepository.save(member);
		}

		@AfterEach
		void tearDown() {
			memberRepository.deleteAllInBatch();
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

	@DisplayName("특정 회원의 회원 정보를 조회하면")
	@Nested
	class GetMemberInfo {

		private Member member;

		@BeforeEach
		void setUp() {
			member = createMember();
			memberRepository.save(member);
		}

		@AfterEach
		void tearDown() {
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("존재하는 회원이라면 해당 회원 정보를 조회한다.")
		@Test
		void withExistsMember() {

			// Given
			Long memberId = member.getId();

			// When
			MemberInfoServiceResponse serviceResponse = memberReadService.getMemberInfo(memberId);

			// Then
			assertThat(serviceResponse)
				.extracting(
					MemberInfoServiceResponse::getMemberId,
					MemberInfoServiceResponse::getName,
					MemberInfoServiceResponse::getNickname,
					MemberInfoServiceResponse::getPhone,
					MemberInfoServiceResponse::getBirthDay,
					MemberInfoServiceResponse::getGender,
					MemberInfoServiceResponse::getProfileImageURL
				)
				.containsExactly(
					member.getId(),
					member.getName(),
					member.getNickname(),
					member.getPhone(),
					member.getBirthDay(),
					member.getGender(),
					member.getProfileImageUrl()
				);
		}

		@DisplayName("존재하지 않는 회원이라면 예외를 발생시킨다.")
		@Test
		void withNotExistsMember() {

			// Given
			Long memberId = member.getId() + 100;

			// When & Then
			assertThatThrownBy(() -> memberReadService.getMemberInfo(memberId))
				.isInstanceOf(MemberNotFoundException.class)
				.hasMessage(DetailMessage.MEMBER_NOT_FOUND);
		}
	}

	@DisplayName("특정 회원의 프로필 이미지를 조회하면")
	@Nested
	class GetMemberProfileImage {

		private Member loginMember;
		private Member partner;
		private Member other;

		@BeforeEach
		void setUp() {
			loginMember = createMember("01012341234", "nickname1", "imageURL1");
			partner = createMember("01011112222", "nickname2", "imageURL2");
			other = createMember("01022223333", "nickname3", "imageURL3");
			memberRepository.saveAll(List.of(loginMember, partner, other));
			MemberThreadLocal.set(loginMember);
		}

		@AfterEach
		void tearDown() {
			memberRepository.deleteAllInBatch();
			MemberThreadLocal.remove();
		}

		@DisplayName("자기 자신의 프로필 이미지를 조회한 경우, 프로필 이미지를 응답한다.")
		@Test
		void withLoginMember() {

			// Given
			Long targetMemberId = loginMember.getId();
			Long partnerMemberId = partner.getId();

			// When
			ProfileImageURLServiceResponse response = memberReadService.getProfileImageURL(
				targetMemberId, partnerMemberId);

			// Then
			assertThat(response.getProfileImageURL())
				.isEqualTo(loginMember.getProfileImageUrl());
		}

		@DisplayName("자신과 연결된 회원의 프로필 이미지를 조회한 경우, 프로필 이미지를 응답한다.")
		@Test
		void withPartnerMember() {

			// Given
			Long targetMemberId = partner.getId();
			Long partnerMemberId = partner.getId();

			// When
			ProfileImageURLServiceResponse response = memberReadService.getProfileImageURL(
				targetMemberId, partnerMemberId);

			// Then
			assertThat(response.getProfileImageURL())
				.isEqualTo(partner.getProfileImageUrl());
		}

		@DisplayName("자신과 연결되지 않은 회원의 프로필 이미지를 조회한 경우, 예외를 발생시킨다.")
		@Test
		void withOtherMember() {

			// Given
			Long targetMemberId = other.getId();
			Long partnerMemberId = partner.getId();
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER, Operation.READ);

			// When & Then
			assertThatThrownBy(() -> memberReadService.getProfileImageURL(targetMemberId, partnerMemberId))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(expectedException.getMessage());
		}

		@DisplayName("특정 회원이 존재하지 않는다면 예외를 발생시킨다.")
		@Test
		void withNotExistsMember() {

			// Given
			Long targetMemberId = loginMember.getId() + 100;
			Long partnerMemberId = partner.getId();
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER, Operation.READ);

			// When & Then
			assertThatThrownBy(() -> memberReadService.getProfileImageURL(targetMemberId, partnerMemberId))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(expectedException.getMessage());
		}
	}

	private Member createMember() {

		return Member.builder()
			.name("홍길동")
			.nickname("nickname")
			.phone("01012345678")
			.password("password")
			.gender(Gender.MALE)
			.birthDay(LocalDate.of(2020, 10, 10))
			.build();
	}

	private Member createMember(String phone, String nickname) {

		return Member.builder()
			.name("홍길동")
			.nickname(nickname)
			.phone(phone)
			.password("password")
			.gender(Gender.FEMALE)
			.birthDay(LocalDate.of(2020, 10, 10))
			.build();
	}

	private Member createMember(String phone, String nickname, String profileImageURL){

		Member member = createMember(phone, nickname);

		member.updateProfileImageUrl(profileImageURL);

		return member;
	}
}
