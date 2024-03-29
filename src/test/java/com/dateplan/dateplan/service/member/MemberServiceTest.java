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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.amazonaws.SdkClientException;
import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.repository.CoupleRepository;
import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.domain.member.controller.dto.request.UpdatePasswordRequest;
import com.dateplan.dateplan.domain.member.controller.dto.response.PresignedURLResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.AuthService;
import com.dateplan.dateplan.domain.member.service.MemberReadService;
import com.dateplan.dateplan.domain.member.service.MemberService;
import com.dateplan.dateplan.domain.member.service.dto.request.CheckPasswordServiceRequest;
import com.dateplan.dateplan.domain.member.service.dto.request.CheckPasswordServiceResponse;
import com.dateplan.dateplan.domain.member.service.dto.request.SignUpServiceRequest;
import com.dateplan.dateplan.domain.member.service.dto.request.UpdatePasswordServiceRequest;
import com.dateplan.dateplan.domain.s3.S3ImageType;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.S3Exception;
import com.dateplan.dateplan.global.exception.S3ImageNotFoundException;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.auth.PhoneNotAuthenticatedException;
import com.dateplan.dateplan.global.exception.member.AlReadyRegisteredNicknameException;
import com.dateplan.dateplan.global.exception.member.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.service.ServiceTestSupport;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import org.jasypt.util.password.PasswordEncryptor;
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

	@Autowired
	private CoupleRepository coupleRepository;

	@SpyBean
	private CoupleService coupleService;

	@Autowired
	private PasswordEncryptor passwordEncryptor;

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
					"birthDay",
					"gender")
				.contains(
					request.getName(),
					request.getPhone(),
					request.getNickname(),
					request.getBirthDay(),
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
			loginMember = createMember("nickname");
			memberRepository.save(loginMember);
		}

		@AfterEach
		void tearDown() {
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
				loginMember,
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
			assertThatThrownBy(
				() -> memberService.getPresignedURLForProfileImage(loginMember, targetMemberId))
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
			assertThatThrownBy(
				() -> memberService.getPresignedURLForProfileImage(loginMember, targetMemberId))
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
			loginMember = createMember("nickname1");
			memberRepository.save(loginMember);
		}

		@AfterEach
		void tearDown() {
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
			memberService.checkAndSaveProfileImage(loginMember, targetMemberId);

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
			assertThatThrownBy(
				() -> memberService.checkAndSaveProfileImage(loginMember, targetMemberId))
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
			Member targetMember = createMember("01000000000", "nickname2",
				"targetMemberProfileImageURL");
			memberRepository.save(targetMember);

			Long targetMemberId = targetMember.getId();
			String savedImageURL = targetMember.getProfileImageUrl();
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.UPDATE);

			// When & Then
			assertThatThrownBy(
				() -> memberService.checkAndSaveProfileImage(loginMember, targetMemberId))
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
			loginMember = createMember("nickname1");
			memberRepository.save(loginMember);
		}

		@AfterEach
		void tearDown() {
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
			memberService.deleteProfileImage(loginMember, targetMemberId);

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
			assertThatThrownBy(() -> memberService.deleteProfileImage(loginMember, targetMemberId))
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
			Member targetMember = createMember("01000000000", "nickname2",
				"targetMemberProfileImageURL");
			memberRepository.save(targetMember);

			Long targetMemberId = targetMember.getId();
			String savedImageURL = targetMember.getProfileImageUrl();
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.DELETE);

			// When & Then
			assertThatThrownBy(() -> memberService.deleteProfileImage(loginMember, targetMemberId))
				.isInstanceOf(NoPermissionException.class)
				.hasMessage(expectedException.getMessage());

			targetMember = memberRepository.findById(targetMemberId).get();

			assertThat(targetMember.getProfileImageUrl())
				.isEqualTo(savedImageURL);

			then(s3Client)
				.shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("비밀번호 확인 시")
	class CheckPassword {

		private Member member;

		@BeforeEach
		void setUp() {
			String password = passwordEncryptor.encryptPassword("password");
			member = memberRepository.save(Member.builder()
				.phone("01011112222")
				.name("name")
				.nickname("nickname")
				.birthDay(LocalDate.now())
				.gender(Gender.MALE)
				.password(password)
				.build()
			);
		}

		@AfterEach
		void tearDown() {
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("[성공] 현재 비밀번호와 일치하는 비밀번호를 요청하면 true를 반환한다.")
		@Test
		void should_returnTrue_When_matchPassword() {

			// Given
			CheckPasswordServiceRequest request = CheckPasswordServiceRequest.builder()
				.password("password")
				.build();

			// When
			CheckPasswordServiceResponse response = memberService.checkPassword(
				member, member.getId(), request);

			// Then
			assertThat(response.getPasswordMatch()).isTrue();
		}

		@DisplayName("[실패] 현재 비밀번호와 일치하지않는 비밀번호를 요청하면 false를 반환한다.")
		@Test
		void should_returnFalse_When_mismatchPassword() {

			// Given
			CheckPasswordServiceRequest request = CheckPasswordServiceRequest.builder()
				.password("abcd1234")
				.build();

			// When
			CheckPasswordServiceResponse response = memberService.checkPassword(
				member, member.getId(), request);

			// Then
			assertThat(response.getPasswordMatch()).isFalse();
		}

		@DisplayName("[실패] 요청한 memberId와 로그인한 회원의 id가 다르면 실패한다.")
		@Test
		void should_throwNoPermissionException_When_mismatchMemberId() {

			// Given
			CheckPasswordServiceRequest request = CheckPasswordServiceRequest.builder()
				.password("abcd1234")
				.build();

			NoPermissionException exception = new NoPermissionException(Resource.MEMBER,
				Operation.READ);
			assertThatThrownBy(
				() -> memberService.checkPassword(member, member.getId() + 100, request))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	@Nested
	@DisplayName("회원 탈퇴 시")
	class Withdrawal {

		private Member member;
		private Member partner;
		private Couple couple;

		@BeforeEach
		void setUp() {
			member = memberRepository.save(createMember("01011112222", "aaa", "url"));
			partner = memberRepository.save(createMember("01011113333", "bbb", "url"));
			couple = coupleRepository.save(Couple.builder()
				.member1(member)
				.member2(partner)
				.firstDate(LocalDate.now())
				.build()
			);
		}

		@AfterEach
		void tearDown() {
			coupleRepository.deleteAllInBatch();
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("[성공] 회원이 연결되어 있지 않은 경우, 회원의 모든 정보가 삭제된다.")
		@Test
		void should_deleteAllInfoRelatedMember_When_memberNotConnected() {

			// Given
			Member other = memberRepository.save(createMember("01011114444", "ccc", "url"));

			// When
			memberService.withdrawal(other, other.getId());

			// Then
			assertThat(memberRepository.findById(other.getId())).isEmpty();

			// Verify
			then(coupleService)
				.shouldHaveNoInteractions();
		}

		@DisplayName("[성공] 회원이 연결되어 있는 경우, 회원 연결을 해제하고 회원의 모든 정보가 삭제된다.")
		@Test
		void should_deleteAllInfoRelatedMember_When_memberConnected() {

			// When
			memberService.withdrawal(member, member.getId());

			// Then
			assertThat(memberRepository.findById(member.getId())).isEmpty();
			assertThat(memberRepository.findById(partner.getId())).isPresent();
			assertThat(coupleRepository.findById(couple.getId())).isEmpty();

			// Verify
			then(coupleService)
				.should(times(1)).disconnectCouple(any(Member.class), anyLong());
		}

		@DisplayName("[실패] 요청한 회원의 id와 로그인한 회원의 id가 다르면 실패한다.")
		@Test
		void should_throwNoPermissionException_When_mismatchMemberId() {

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.MEMBER,
				Operation.DELETE);
			assertThatThrownBy(() -> memberService.withdrawal(member, member.getId() + 100))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());

			// Verify
			then(coupleService)
				.shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("비밀번호 변경 시")
	class UpdatePassword {

		private Member member;

		@BeforeEach
		void setUp() {
			String password = passwordEncryptor.encryptPassword("password");
			member = memberRepository.save(Member.builder()
				.phone("01011112222")
				.name("name")
				.nickname("nickname")
				.birthDay(LocalDate.now())
				.gender(Gender.MALE)
				.password(password)
				.build()
			);
		}

		@AfterEach
		void tearDown() {
			memberRepository.deleteAllInBatch();
		}

		@DisplayName("[성공] 올바른 패턴의 비밀번호가 입력되면 비밀번호가 변경된다.")
		@Test
		void should_updatePassword_When_inputValidPassword() {

			// Given
			UpdatePasswordServiceRequest request = UpdatePasswordServiceRequest.builder()
				.password("newPassword")
				.build();

			// When
			memberService.updatePassword(member, member.getId(), request);

			// Then
			member = memberRepository.findById(member.getId()).get();
			assertThat(passwordEncryptor.checkPassword(request.getPassword(), member.getPassword()))
				.isTrue();
		}

		@DisplayName("[실패] 요청한 회원의 id와 로그인한 회원의 id가 다르면 실패한다")
		@Test
		void should_throwNoPermissionException_When_mismatchMemberId() {

			// Given
			UpdatePasswordServiceRequest request = UpdatePasswordServiceRequest.builder()
				.password("newPassword")
				.build();

			// When & Then
			NoPermissionException exception = new NoPermissionException(Resource.MEMBER,
				Operation.UPDATE);
			assertThatThrownBy(
				() -> memberService.updatePassword(member, member.getId() + 100, request))
				.isInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		}
	}

	private Member createMember(String nickname) {

		return Member.builder()
			.name("홍길동")
			.nickname(nickname)
			.phone("01012341234")
			.password("password")
			.gender(Gender.MALE)
			.birthDay(LocalDate.of(1999, 10, 10))
			.build();
	}

	private Member createMember(String phone, String nickname, String profileImageURL) {

		Member member = Member.builder()
			.name("홍길동")
			.nickname(nickname)
			.phone(phone)
			.password("password")
			.gender(Gender.MALE)
			.birthDay(LocalDate.of(1999, 10, 10))
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
			.birthDay(LocalDate.of(1999, 10, 10))
			.build();
	}
}
