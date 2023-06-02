package com.dateplan.dateplan.service.member;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_REGISTERED_NICKNAME;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_REGISTERED_PHONE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.NOT_AUTHENTICATED_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.dateplan.dateplan.domain.member.dto.SignUpServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredNicknameException;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.global.exception.PhoneNotAuthenticatedException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

public class MemberServiceTest extends ServiceTestSupport {

	@Autowired
	private MemberService memberService;

	@SpyBean
	private MemberRepository memberRepository;

	@MockBean
	private MemberReadService memberReadService;

	@MockBean
	private AuthService authService;

	@DisplayName("회원가입시")
	@Nested
	class SignUp {

		@DisplayName("해당 닉네임, 전화번호로 가입된 회원이 없고, 휴대폰 인증을 거쳤다면 회원가입에 성공한다.")
		@Test
		void withValidInputs() {

			// Given
			String phone = "01011112222";
			String nickname = "asdasd";
			SignUpServiceRequest request = createSignUpServiceRequest(phone, nickname);

			// When
			memberService.signUp(request);

			// Then
			then(memberReadService).should(times(1)).throwIfPhoneExists(phone);
			then(memberReadService).should(times(1)).throwIfNicknameExists(nickname);
			then(authService).should(times(1)).throwIfPhoneNotAuthenticated(phone);
			then(authService).should(times(1)).deleteAuthenticationInfoInRedis(phone);
			then(memberRepository).should(times(1)).save(any(Member.class));

			Member savedMember = memberRepository.findByPhone(phone).orElse(null);
			assertThat(savedMember).isNotNull();
			assertThat(savedMember.getId()).isNotNull();
			assertThat(savedMember).extracting("name", "phone", "nickname", "birth", "gender")
				.contains(request.getName(), request.getPhone(), request.getNickname(),
					request.getBirth(), request.getGender());
		}

		@DisplayName("이미 가입된 전화번호일 때, 회원가입에 실패한다.")
		@Test
		void withDuplicatedPhone() {

			// Given
			String phone = "01012341234";
			String nickname = "asdasd";
			SignUpServiceRequest request = createSignUpServiceRequest(phone, nickname);

			// Stub
			AlReadyRegisteredPhoneException expectedException = new AlReadyRegisteredPhoneException();
			willThrow(expectedException).given(memberReadService).throwIfPhoneExists(anyString());

			// When
			assertThatThrownBy(() -> memberService.signUp(request))
				.isInstanceOf(AlReadyRegisteredPhoneException.class)
				.hasMessage(ALREADY_REGISTERED_PHONE);

			// Then
			then(memberReadService).should(times(1)).throwIfPhoneExists(phone);
			then(memberReadService).should(never()).throwIfNicknameExists(anyString());
			then(authService).should(never()).throwIfPhoneNotAuthenticated(anyString());
			then(authService).should(never()).deleteAuthenticationInfoInRedis(anyString());
			then(memberRepository).should(never()).save(any(Member.class));
		}

		@DisplayName("이미 가입된 닉네임일 때, 회원가입에 실패한다.")
		@Test
		void withDuplicatedNickname() {

			// Given
			String phone = "01012341234";
			String nickname = "asdasd";
			SignUpServiceRequest request = createSignUpServiceRequest(phone, nickname);

			// Stub
			AlReadyRegisteredNicknameException expectedException = new AlReadyRegisteredNicknameException();
			willThrow(expectedException).given(memberReadService).throwIfNicknameExists(anyString());

			// When
			assertThatThrownBy(() -> memberService.signUp(request))
				.isInstanceOf(AlReadyRegisteredNicknameException.class)
				.hasMessage(ALREADY_REGISTERED_NICKNAME);

			// Then
			then(memberReadService).should(times(1)).throwIfPhoneExists(phone);
			then(memberReadService).should(times(1)).throwIfNicknameExists(nickname);
			then(authService).should(never()).throwIfPhoneNotAuthenticated(anyString());
			then(authService).should(never()).deleteAuthenticationInfoInRedis(anyString());
			then(memberRepository).should(never()).save(any(Member.class));
		}

		@DisplayName("전화번호 인증이 되어있지 않을 때, 회원가입에 실패한다.")
		@Test
		void notAuthenticatedPhone() {

			// Given
			String phone = "01012341234";
			String nickname = "asdasd";
			SignUpServiceRequest request = createSignUpServiceRequest(phone, nickname);

			// Stub
			PhoneNotAuthenticatedException expectedException = new PhoneNotAuthenticatedException();
			willThrow(expectedException).given(authService).throwIfPhoneNotAuthenticated(anyString());

			// When
			assertThatThrownBy(() -> memberService.signUp(request))
				.isInstanceOf(PhoneNotAuthenticatedException.class)
				.hasMessage(NOT_AUTHENTICATED_PHONE);

			// Then
			then(memberReadService).should(times(1)).throwIfPhoneExists(phone);
			then(memberReadService).should(times(1)).throwIfNicknameExists(nickname);
			then(authService).should(times(1)).throwIfPhoneNotAuthenticated(phone);
			then(authService).should(never()).deleteAuthenticationInfoInRedis(anyString());
			then(memberRepository).should(never()).save(any(Member.class));
		}
	}

	private SignUpServiceRequest createSignUpServiceRequest(String phone, String nickname) {

		return SignUpServiceRequest.builder()
			.name("홍길동")
			.nickname(nickname)
			.phone(phone)
			.password("password")
			.gender(Gender.MALE)
			.birth(LocalDate.of(1999, 10, 10))
			.build();
	}
}
