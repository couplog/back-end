package com.dateplan.dateplan.domain.member.entity;

import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.converter.PasswordConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member {

	public static final String DEFAULT_PROFILE_IMAGE = "https://date-plan.s3.ap-northeast-2.amazonaws.com/members/profile/%E1%84%8E%E1%85%AE%E1%86%AB%E1%84%89%E1%85%B5%E1%86%A8%E1%84%8A%E1%85%B3.jpeg";

	@Id
	@Column(name = "member_id", columnDefinition = "BIGINT", updatable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@Column(name = "name", columnDefinition = "VARCHAR(30)")
	private String name;

	@NotNull
	@Column(name = "phone", columnDefinition = "VARCHAR(20)", unique = true)
	private String phone;

	@NotNull
	@Column(name = "nickname", unique = true, columnDefinition = "VARCHAR(30)")
	private String nickname;

	@NotNull
	@Column(name = "birth_day", columnDefinition = "DATE", updatable = false)
	private LocalDate birthDay;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", columnDefinition = "VARCHAR(10)")
	private Gender gender;

	@NotNull
	@Column(name = "profile_image_url", columnDefinition = "VARCHAR(200)")
	private String profileImageUrl = DEFAULT_PROFILE_IMAGE;

	@Convert(converter = PasswordConverter.class)
	@NotNull
	@Column(name = "password", columnDefinition = "VARCHAR(100)")
	private String password;

	@Builder
	public Member(
		String name,
		String phone,
		String nickname,
		LocalDate birthDay,
		Gender gender,
		String password
	) {
		this.name = name;
		this.phone = phone;
		this.nickname = nickname;
		this.birthDay = birthDay;
		this.gender = gender;
		this.profileImageUrl = DEFAULT_PROFILE_IMAGE;
		this.password = password;
	}

	public void updateProfileImageUrl(String profileImageUrl){

		this.profileImageUrl = profileImageUrl;
	}
}
