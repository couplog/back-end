package com.dateplan.dateplan.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "member")
public class Member {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@NotNull
		@Column(name = "name", length = 30)
		private String name;

		@NotNull
		@Column(name = "phone", length = 20)
		private String phone;

		@Column(name = "birth")
		private LocalDate birth;

		@Column(name = "gender", length = 20)
		private String gender;

		@NotNull
		@Column(name = "profile_image_url", length = 200)
		private String profileImageUrl;

		@Builder
		public Member(
				String name,
				String phone,
				LocalDate birth,
				String gender,
				String profileImageUrl
		) {
				this.name = name;
				this.phone = phone;
				this.birth = birth;
				this.gender = gender;
				this.profileImageUrl = profileImageUrl;
		}
}
