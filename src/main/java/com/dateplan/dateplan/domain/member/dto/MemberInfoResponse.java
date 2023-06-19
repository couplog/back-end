package com.dateplan.dateplan.domain.member.dto;

import com.dateplan.dateplan.domain.member.entity.Member;
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
	private LocalDate birthDay;
	private Gender gender;
	private String profileImageURL;
	private boolean isConnected;

	public static MemberInfoResponse from(Member member, boolean isConnected){

		return MemberInfoResponse.builder()
			.memberId(member.getId())
			.name(member.getName())
			.nickname(member.getNickname())
			.phone(member.getPhone())
			.birthDay(member.getBirthDay())
			.gender(member.getGender())
			.profileImageURL(member.getProfileImageUrl())
			.isConnected(isConnected)
			.build();
	}
}
