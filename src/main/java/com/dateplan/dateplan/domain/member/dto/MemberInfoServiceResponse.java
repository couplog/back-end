package com.dateplan.dateplan.domain.member.dto;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.constant.Gender;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfoServiceResponse {

	private Long memberId;
	private String name;
	private String nickname;
	private String phone;
	private LocalDate birthDay;
	private Gender gender;
	private String profileImageURL;

	public static MemberInfoServiceResponse from(Member member){

		return MemberInfoServiceResponse.builder()
			.memberId(member.getId())
			.name(member.getName())
			.nickname(member.getNickname())
			.phone(member.getPhone())
			.birthDay(member.getBirthDay())
			.gender(member.getGender())
			.profileImageURL(member.getProfileImageUrl())
			.build();
	}

	public MemberInfoResponse toResponse(boolean isConnected){

		return MemberInfoResponse.builder()
			.memberId(this.memberId)
			.name(this.name)
			.nickname(this.nickname)
			.phone(this.phone)
			.birthDay(this.birthDay)
			.gender(this.gender)
			.profileImageURL(this.profileImageURL)
			.isConnected(isConnected)
			.build();
	}
}
