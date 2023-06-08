package com.dateplan.dateplan.service.member;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_REGISTERED_NICKNAME;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_REGISTERED_PHONE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.NOT_AUTHENTICATED_PHONE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.S3_CREATE_PRESIGNED_URL_FAIL;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.S3_DELETE_OBJECT_FAIL;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.S3_IMAGE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.amazonaws.SdkClientException;
import com.dateplan.dateplan.domain.member.dto.PresignedURLResponse;
import com.dateplan.dateplan.domain.member.dto.signup.SignUpServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.domain.s3.S3ImageType;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredNicknameException;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.global.exception.NoPermissionException;
import com.dateplan.dateplan.global.exception.PhoneNotAuthenticatedException;
import com.dateplan.dateplan.global.exception.S3Exception;
import com.dateplan.dateplan.global.exception.S3ImageNotFoundException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
			then(memberReadService)
				.should(times(1))
				.throwIfPhoneExists(phone);
			then(memberReadService)
				.should(times(1))
				.throwIfNicknameExists(nickname);
			then(authService)
				.should(times(1))
				.throwIfPhoneNotAuthenticated(phone);
			then(authService)
				.should(times(1))
				.deleteAuthenticationInfoInRedis(phone);
			then(memberRepository)
				.should(times(1))
				.save(any(Member.class));

			Member savedMember = memberRepository.findByPhone(phone).orElse(null);
			assertThat(savedMember).isNotNull();
			assertThat(savedMember.getId()).isNotNull();
			assertThat(savedMember).extracting(
					"name",
					"phone",
					"nickname",
					"birth",
					"gender")
				.contains(
					request.getName(),
					request.getPhone(),
					request.getNickname(),
					request.getBirth(),
					request.getGender());
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
			willThrow(expectedException)
				.given(memberReadService)
				.throwIfPhoneExists(anyString());

			// When
			assertThatThrownBy(() -> memberService.signUp(request))
				.isInstanceOf(AlReadyRegisteredPhoneException.class)
				.hasMessage(ALREADY_REGISTERED_PHONE);

			// Then
			then(memberReadService)
				.should(times(1))
				.throwIfPhoneExists(phone);
			then(memberReadService)
				.should(never())
				.throwIfNicknameExists(anyString());
			then(authService)
				.should(never())
				.throwIfPhoneNotAuthenticated(anyString());
			then(authService)
				.should(never())
				.deleteAuthenticationInfoInRedis(anyString());
			then(memberRepository)
				.should(never())
				.save(any(Member.class));
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
			willThrow(expectedException)
				.given(memberReadService)
				.throwIfNicknameExists(anyString());

			// When
			assertThatThrownBy(() -> memberService.signUp(request))
				.isInstanceOf(AlReadyRegisteredNicknameException.class)
				.hasMessage(ALREADY_REGISTERED_NICKNAME);

			// Then
			then(memberReadService)
				.should(times(1))
				.throwIfPhoneExists(phone);
			then(memberReadService)
				.should(times(1))
				.throwIfNicknameExists(nickname);
			then(authService)
				.should(never())
				.throwIfPhoneNotAuthenticated(anyString());
			then(authService)
				.should(never())
				.deleteAuthenticationInfoInRedis(anyString());
			then(memberRepository)
				.should(never())
				.save(any(Member.class));
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
			willThrow(expectedException)
				.given(authService)
				.throwIfPhoneNotAuthenticated(anyString());

			// When
			assertThatThrownBy(() -> memberService.signUp(request))
				.isInstanceOf(PhoneNotAuthenticatedException.class)
				.hasMessage(NOT_AUTHENTICATED_PHONE);

			// Then
			then(memberReadService)
				.should(times(1))
				.throwIfPhoneExists(phone);
			then(memberReadService)
				.should(times(1))
				.throwIfNicknameExists(nickname);
			then(authService)
				.should(times(1))
				.throwIfPhoneNotAuthenticated(phone);
			then(authService)
				.should(never())
				.deleteAuthenticationInfoInRedis(anyString());
			then(memberRepository)
				.should(never())
				.save(any(Member.class));
		}
	}

	@DisplayName("Presigned URL 요청시")
	@Nested
	class getPresignedURL {

		private Member loginMember;

		@BeforeEach
		void setUp() {
			loginMember = createMember();
			memberRepository.save(loginMember);
			MemberThreadLocal.set(loginMember);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("s3 client 로부터 예외가 발생하지 않고, url 을 잘 받아 온다면 URL 정보를 리턴한다.")
		@Test
		void withNoExceptionThrowsS3Client() throws MalformedURLException {

			// Given
			Long targetMemberId = loginMember.getId();

			// Stub
			URL expectedURL = new URL("https://something.com");
			given(s3Client.getPreSignedUrl(any(S3ImageType.class), anyString()))
				.willReturn(expectedURL);

			// When
			PresignedURLResponse response = memberService.getPresignedURLForProfileImage(
				targetMemberId);

			// Then
			assertThat(response.getPresignedURL()).isEqualTo(expectedURL.toString());
			then(s3Client)
				.should(times(1))
				.getPreSignedUrl(any(S3ImageType.class), anyString());
		}

		@DisplayName("s3 client 로부터 예외가 발생한다면, 해당 예외를 그대로 던진다.")
		@Test
		void withExceptionThrowsS3Client() {

			// Given
			Long targetMemberId = loginMember.getId();

			// Stub
			SdkClientException sdkClientException = new SdkClientException("message");
			S3Exception expectedException = new S3Exception(S3_CREATE_PRESIGNED_URL_FAIL,
				sdkClientException);
			given(s3Client.getPreSignedUrl(any(S3ImageType.class), anyString()))
				.willThrow(expectedException);

			// When & Then
			assertThatThrownBy(() -> memberService.getPresignedURLForProfileImage(targetMemberId))
				.isInstanceOf(S3Exception.class)
				.hasMessage(S3_CREATE_PRESIGNED_URL_FAIL)
				.hasCauseInstanceOf(SdkClientException.class);

			then(s3Client)
				.should(times(1))
				.getPreSignedUrl(any(S3ImageType.class), anyString());
		}

		@DisplayName("현재 로그인한 회원과 Presigned URL 조회 대상 회원이 다르다면 예외를 발생시킨다.")
		@Test
		void withNoPermission() {

			// Given
			Long targetMemberId = loginMember.getId() + 1L;
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.READ);

			// When & Then
			assertThatThrownBy(() -> memberService.getPresignedURLForProfileImage(targetMemberId))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(expectedException.getMessage());

			then(s3Client)
				.shouldHaveNoInteractions();
		}
	}

	@DisplayName("회원 프로필 이미지 수정 요청시")
	@Nested
	class CheckAndSaveProfileImage {

		private Member loginMember;

		@BeforeEach
		void setUp() {
			loginMember = createMember();
			memberRepository.save(loginMember);
			MemberThreadLocal.set(loginMember);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("이미지가 s3 에 존재한다면 회원의 profileImage 가 변경된다.")
		@Test
		void withExistsImageInS3() throws MalformedURLException {

			// Given
			Long targetMemberId = loginMember.getId();

			// Stub
			URL expectedURL = new URL("https://something.com");
			doNothing()
				.when(s3Client)
				.throwIfImageNotFound(any(S3ImageType.class), anyString());
			given(s3Client.getObjectUrl(any(S3ImageType.class), anyString()))
				.willReturn(expectedURL);

			// When
			memberService.checkAndSaveProfileImage(targetMemberId);

			// Then
			then(s3Client)
				.should(times(1))
				.throwIfImageNotFound(any(S3ImageType.class), anyString());
			then(s3Client)
				.should(times(1))
				.getObjectUrl(any(S3ImageType.class), anyString());

			Member targetMember = memberRepository.findById(targetMemberId).get();

			assertThat(targetMember.getProfileImageUrl())
				.isEqualTo(expectedURL.toString());
		}

		@DisplayName("이미지가 s3 에 존재하지 않는다면 예외를 발생시키고, 회원의 profileImage 가 변경되지 않는다.")
		@Test
		void withNotExistsImageInS3() {

			// Given
			Long targetMemberId = loginMember.getId();
			String savedImageURL = loginMember.getProfileImageUrl();

			// Stub
			S3ImageNotFoundException expectedException = new S3ImageNotFoundException();
			willThrow(expectedException)
				.given(s3Client)
				.throwIfImageNotFound(any(S3ImageType.class), anyString());

			// When & Then
			assertThatThrownBy(() -> memberService.checkAndSaveProfileImage(targetMemberId))
				.isInstanceOf(S3ImageNotFoundException.class)
				.hasMessage(S3_IMAGE_NOT_FOUND);

			Member targetMember = memberRepository.findById(targetMemberId).get();

			assertThat(targetMember.getProfileImageUrl())
				.isEqualTo(savedImageURL);

			then(s3Client)
				.should(times(1))
				.throwIfImageNotFound(any(S3ImageType.class), anyString());
			then(s3Client)
				.should(never())
				.getObjectUrl(any(S3ImageType.class), anyString());
		}

		@DisplayName("현재 로그인한 회원과 프로필 이미지 수정 대상 회원이 다르다면 예외를 발생시키고, target 회원의 profileImage 가 변경되지 않는다..")
		@Test
		void withNoPermission() {

			// Given
			Member targetMember = createMember("01000000000", "targetMemberProfileImageURL");
			memberRepository.save(targetMember);

			Long targetMemberId = targetMember.getId();
			String savedImageURL = targetMember.getProfileImageUrl();
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.UPDATE);

			// When & Then
			assertThatThrownBy(() -> memberService.checkAndSaveProfileImage(targetMemberId))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(expectedException.getMessage());

			targetMember = memberRepository.findById(targetMemberId).get();

			assertThat(targetMember.getProfileImageUrl())
				.isEqualTo(savedImageURL);

			then(s3Client)
				.shouldHaveNoInteractions();
		}
	}

	@DisplayName("회원 프로필 이미지 삭제 요청시")
	@Nested
	class DeleteProfileImage {

		private Member loginMember;

		@BeforeEach
		void setUp() {
			loginMember = createMember();
			memberRepository.save(loginMember);
			MemberThreadLocal.set(loginMember);
		}

		@AfterEach
		void tearDown() {
			MemberThreadLocal.remove();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("S3 로 정상적인 요청 및 응답이 온다면 회원의 profileImage 가 기본 이미지로 변경된다.")
		@Test
		void withAvailableS3() {

			// Given
			Long targetMemberId = loginMember.getId();

			// Stub
			willDoNothing()
				.given(s3Client)
				.deleteObject(any(S3ImageType.class), anyString());

			// When
			memberService.deleteProfileImage(targetMemberId);

			// Then
			then(s3Client)
				.should(times(1))
				.deleteObject(any(S3ImageType.class), anyString());

			Member findMember = memberRepository.findById(targetMemberId).get();

			assertThat(findMember.getProfileImageUrl())
				.isEqualTo(Member.DEFAULT_PROFILE_IMAGE);
		}

		@DisplayName("S3 로 요청 및 응답 과정에 문제가 있다면 예외가 발생하고 회원의 profileImage 가 변경되지 않는다.")
		@Test
		void withUnAvailableS3() {

			// Given
			Long targetMemberId = loginMember.getId();
			String savedImageURL = loginMember.getProfileImageUrl();

			// Stub
			SdkClientException sdkClientException = new SdkClientException("message");
			S3Exception expectedException = new S3Exception(S3_DELETE_OBJECT_FAIL,
				sdkClientException);
			willThrow(expectedException)
				.given(s3Client)
				.deleteObject(any(S3ImageType.class), anyString());

			// When
			assertThatThrownBy(() -> memberService.deleteProfileImage(targetMemberId))
				.isInstanceOf(S3Exception.class)
				.hasMessage(S3_DELETE_OBJECT_FAIL)
				.hasCauseInstanceOf(SdkClientException.class);

			Member findMember = memberRepository.findById(targetMemberId).get();

			assertThat(findMember.getProfileImageUrl())
				.isEqualTo(savedImageURL);

			then(s3Client)
				.should(times(1))
				.deleteObject(any(S3ImageType.class), anyString());
		}

		@DisplayName("현재 로그인한 회원과 프로필 이미지 삭제 대상 회원이 다르다면 예외를 발생시키고, target 회원의 profileImage 가 변경되지 않는다.")
		@Test
		void withNoPermission() {

			// Given
			Member targetMember = createMember("01000000000", "targetMemberProfileImageURL");
			memberRepository.save(targetMember);

			Long targetMemberId = targetMember.getId();
			String savedImageURL = targetMember.getProfileImageUrl();
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.DELETE);

			// When & Then
			assertThatThrownBy(() -> memberService.deleteProfileImage(targetMemberId))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(expectedException.getMessage());

			targetMember = memberRepository.findById(targetMemberId).get();

			assertThat(targetMember.getProfileImageUrl())
				.isEqualTo(savedImageURL);

			then(s3Client)
				.shouldHaveNoInteractions();
		}
	}

	private Member createMember() {

		return Member.builder()
			.name("홍길동")
			.nickname("nickname")
			.phone("01012341234")
			.password("password")
			.gender(Gender.MALE)
			.birth(LocalDate.of(1999, 10, 10))
			.build();
	}

	private Member createMember(String phone, String profileImageURL) {

		Member member = Member.builder()
			.name("홍길동")
			.nickname("nickname")
			.phone(phone)
			.password("password")
			.gender(Gender.MALE)
			.birth(LocalDate.of(1999, 10, 10))
			.build();

		member.updateProfileImageUrl(profileImageURL);

		return member;
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
