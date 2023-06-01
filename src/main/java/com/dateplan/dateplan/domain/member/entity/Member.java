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
	@Column(name = "nickname", columnDefinition = "VARCHAR(30)")
	private String nickname;

	@Column(name = "birth", columnDefinition = "DATE", updatable = false)
	private LocalDate birth;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", columnDefinition = "VARCHAR(10)")
	private Gender gender;

	//@NotNull
	@Column(name = "profile_image_url", columnDefinition = "VARCHAR(200)")
	private String profileImageUrl;

	@Convert(converter = PasswordConverter.class)
	@NotNull
	@Column(name = "password", columnDefinition = "VARCHAR(100)")
	private String password;

	@Builder
	public Member(
		String name,
		String phone,
		String nickname,
		LocalDate birth,
		Gender gender,
		String profileImageUrl,
		String password
	) {
		this.name = name;
		this.phone = phone;
		this.nickname = nickname;
		this.birth = birth;
		this.gender = gender;
		this.profileImageUrl = profileImageUrl;
		this.password = password;
	}
}
