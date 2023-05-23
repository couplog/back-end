package com.dateplan.dateplan.domain.member.service;

import com.dateplan.dateplan.domain.member.repository.MemberRepository;
import com.dateplan.dateplan.global.exception.AlReadyRegisteredPhoneException;
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
}
