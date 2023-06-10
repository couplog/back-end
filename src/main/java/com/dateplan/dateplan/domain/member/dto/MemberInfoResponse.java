package com.dateplan.dateplan.domain.member.dto;

import com.dateplan.dateplan.global.constant.Gender;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfoResponse {

	private Long memberId;
	private String name;
	private String nickname;
	private String phone;
	private LocalDate birth;
	private Gender gender;
	private String profileImageURL;
	private boolean isConnected;
}
