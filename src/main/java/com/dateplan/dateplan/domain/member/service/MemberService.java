package com.dateplan.dateplan.domain.member.service;

import com.dateplan.dateplan.domain.member.controller.dto.response.PresignedURLResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.member.service.dto.request.SignUpServiceRequest;
import com.dateplan.dateplan.domain.s3.S3Client;
import com.dateplan.dateplan.domain.s3.S3ImageType;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import java.net.URL;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final MemberReadService memberReadService;
	private final AuthService authService;
	private final S3Client s3Client;

	public void signUp(SignUpServiceRequest request) {

		String phone = request.getPhone();
		String nickname = request.getNickname();

		memberReadService.throwIfPhoneExists(phone);
		memberReadService.throwIfNicknameExists(nickname);

		authService.throwIfPhoneNotAuthenticated(phone);

		Member member = request.toMember();
		memberRepository.save(member);

		authService.deleteAuthenticationInfoInRedis(phone);
	}

	public PresignedURLResponse getPresignedURLForProfileImage(Member loginMember, Long memberId) {

		if (!isSameMember(memberId, loginMember.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.READ);
		}

		URL preSignedUrl = s3Client.getPreSignedUrl(S3ImageType.MEMBER_PROFILE,
			loginMember.getId().toString());

		return PresignedURLResponse.builder()
			.presignedURL(preSignedUrl.toString())
			.build();
	}

	public void checkAndSaveProfileImage(Member loginMember, Long memberId) {

		if (!isSameMember(memberId, loginMember.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.UPDATE);
		}

		String memberIdStr = loginMember.getId().toString();

		s3Client.throwIfImageNotFound(S3ImageType.MEMBER_PROFILE, memberIdStr);
		URL url = s3Client.getObjectUrl(S3ImageType.MEMBER_PROFILE, memberIdStr);

		loginMember.updateProfileImageUrl(url.toString());

		memberRepository.save(loginMember);
	}

	public void deleteProfileImage(Member loginMember, Long memberId) {

		if (!isSameMember(memberId, loginMember.getId())) {
			throw new NoPermissionException(Resource.MEMBER, Operation.DELETE);
		}

		s3Client.deleteObject(S3ImageType.MEMBER_PROFILE, loginMember.getId().toString());
		loginMember.updateProfileImageUrl(Member.DEFAULT_PROFILE_IMAGE);

		memberRepository.save(loginMember);
	}

	private boolean isSameMember(Long memberId, Long loginMemberId) {

		return Objects.equals(memberId, loginMemberId);
	}
}
