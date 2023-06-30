package com.dateplan.dateplan.domain.member.service.dto.request;

import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.constant.Gender;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignUpServiceRequest {

	private String phone;
	private String name;
	private String nickname;
	private String password;
	private LocalDate birthDay;
	private Gender gender;

	public Member toMember(){

		return Member.builder()
			.phone(this.phone)
			.name(this.name)
			.nickname(this.nickname)
			.password(this.password)
			.birthDay(this.birthDay)
			.gender(this.gender)
			.build();
	}
}
