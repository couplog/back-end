package com.dateplan.dateplan.domain.member.service;

import static com.dateplan.dateplan.global.constant.Auth.ACCESS_TOKEN_EXPIRATION;
import static com.dateplan.dateplan.global.constant.Auth.HEADER_AUTHORIZATION;
import static com.dateplan.dateplan.global.constant.Auth.HEADER_REFRESH_TOKEN;
import static com.dateplan.dateplan.global.constant.Auth.REFRESH_TOKEN_EXPIRATION;
import static com.dateplan.dateplan.global.constant.Auth.SUBJECT_ACCESS_TOKEN;
import static com.dateplan.dateplan.global.constant.Auth.SUBJECT_REFRESH_TOKEN;
import static com.dateplan.dateplan.global.exception.ErrorCode.PASSWORD_MISMATCH;
import static com.dateplan.dateplan.global.exception.ErrorCode.USER_NOT_FOUND;

import com.dateplan.dateplan.domain.member.dto.LoginServiceRequest;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.auth.JwtProvider;
import com.dateplan.dateplan.global.exception.ApplicationException;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final PasswordEncryptor passwordEncryptor;
	private final MemberRepository memberRepository;
	private final JwtProvider jwtProvider;

	public void login(LoginServiceRequest request, HttpServletResponse response) {
		Member member = memberRepository.findByPhone(request.getPhone())
			.orElseThrow(
				() -> new ApplicationException(DetailMessage.USER_NOT_FOUND, USER_NOT_FOUND));

		if (mismatchPassword(request, member)) {
			throw new ApplicationException(DetailMessage.PASSWORD_MISMATCH, PASSWORD_MISMATCH);
		}

		String accessToken = "Bearer " + jwtProvider.generateToken(
			member.getId(),
			ACCESS_TOKEN_EXPIRATION.getExpiration(),
			SUBJECT_ACCESS_TOKEN.getContent());
		String refreshToken = "Bearer " + jwtProvider.generateToken(
			member.getId(),
			REFRESH_TOKEN_EXPIRATION.getExpiration(),
			SUBJECT_REFRESH_TOKEN.getContent());

		response.setHeader(HEADER_AUTHORIZATION.getContent(), accessToken);
		response.setHeader(HEADER_REFRESH_TOKEN.getContent(), refreshToken);

		// redis에 저장해야함
	}

	private boolean mismatchPassword(LoginServiceRequest request, Member member) {
		return !passwordEncryptor.checkPassword(request.getPassword(), member.getPassword());
	}
}
