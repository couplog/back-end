package com.dateplan.dateplan.domain.member.service;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredNicknameException;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredPhoneException;
import com.dateplan.dateplan.global.exception.auth.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberReadService {

	private final MemberRepository memberRepository;

	public void throwIfPhoneExists(String phone) {
		if (memberRepository.existsByPhone(phone)) {
			throw new AlReadyRegisteredPhoneException();
		}
	}

	public void throwIfNicknameExists(String nickname) {
		if (memberRepository.existsByNickname(nickname)) {
			throw new AlReadyRegisteredNicknameException();
		}
	}

	public Member findMemberByPhoneOrElseThrow(String phone) {

		return memberRepository.findByPhone(phone)
			.orElseThrow(MemberNotFoundException::new);
	}

	public Member findMemberByIdOrElseThrow(Long id) {
		return memberRepository.findById(id)
			.orElseThrow(MemberNotFoundException::new);
	}
}
