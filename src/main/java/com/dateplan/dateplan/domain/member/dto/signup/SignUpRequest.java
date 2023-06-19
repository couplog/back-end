package com.dateplan.dateplan.domain.member.dto.signup;

import static com.dateplan.dateplan.global.constant.InputPattern.*;
import static com.dateplan.dateplan.global.constant.InputPattern.PHONE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.*;

import com.dateplan.dateplan.global.constant.Gender;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignUpRequest {

	@NotNull(message = INVALID_PHONE_PATTERN)
	@Pattern(regexp = PHONE_PATTERN, message = INVALID_PHONE_PATTERN)
	private String phone;

	@NotNull(message = INVALID_MEMBER_NAME_PATTERN)
	@Pattern(regexp = MEMBER_NAME_PATTERN, message = INVALID_MEMBER_NAME_PATTERN)
	private String name;

	@NotNull(message = INVALID_NICKNAME_PATTERN)
	@Pattern(regexp = NICKNAME_PATTERN, message = INVALID_NICKNAME_PATTERN)
	private String nickname;

	@NotNull(message = INVALID_PASSWORD_PATTERN)
	@Pattern(regexp = PASSWORD_PATTERN, message = INVALID_PASSWORD_PATTERN)
	private String password;

	@DateTimeFormat(iso = ISO.DATE)
	@Past(message = INVALID_BIRTH_RANGE)
	@NotNull(message = INVALID_DATE_PATTERN)
	private LocalDate birthDay;

	@NotNull(message = INVALID_GENDER)
	private Gender gender;

	public SignUpServiceRequest toServiceRequest() {

		return SignUpServiceRequest.builder()
			.phone(this.phone)
			.name(this.name)
			.nickname(this.nickname)
			.password(this.password)
			.birthDay(this.birthDay)
			.gender(this.gender)
			.build();
	}
}
