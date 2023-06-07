package com.dateplan.dateplan.domain.member.service;

import com.dateplan.dateplan.domain.member.dto.PresignedURLResponse;
import com.dateplan.dateplan.domain.member.dto.SignUpServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.domain.s3.S3Client;
import com.dateplan.dateplan.domain.s3.S3ImageType;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import java.net.URL;
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

	public PresignedURLResponse getPresignedURLForProfileImage() {

		Member member = MemberThreadLocal.get();

		URL preSignedUrl = s3Client.getPreSignedUrl(S3ImageType.MEMBER_PROFILE, member.getId().toString());

		return PresignedURLResponse.builder()
			.presignedURL(preSignedUrl.toString())
			.build();
	}

	public void checkAndSaveProfileImage() {

		Member member = MemberThreadLocal.get();
		String memberIdStr = member.getId().toString();

		s3Client.throwIfImageNotFound(S3ImageType.MEMBER_PROFILE, memberIdStr);
		URL url = s3Client.getObjectUrl(S3ImageType.MEMBER_PROFILE, memberIdStr);

		member.updateProfileImageUrl(url.toString());

		memberRepository.save(member);
	}

	public void deleteProfileImage() {

		Member member = MemberThreadLocal.get();

		s3Client.deleteObject(S3ImageType.MEMBER_PROFILE, member.getId().toString());
		member.updateProfileImageUrl(Member.DEFAULT_PROFILE_IMAGE);

		memberRepository.save(member);
	}
}
