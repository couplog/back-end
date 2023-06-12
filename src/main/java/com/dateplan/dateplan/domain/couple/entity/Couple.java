package com.dateplan.dateplan.domain.couple.entity;

import com.dateplan.dateplan.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "couple")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Couple {

	@Id
	@Column(name = "couple_id", columnDefinition = "BIGINT", updatable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@Column(name = "first_date", columnDefinition = "DATE")
	private LocalDate firstDate;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id_1", nullable = false)
	private Member member1;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id_2", nullable = false)
	private Member member2;

	@Builder
	public Couple(LocalDate firstDate, Member member1, Member member2) {
		this.firstDate = firstDate;
		this.member1 = member1;
		this.member2 = member2;
	}

	public void updateFirstDate(LocalDate firstDate) {
		this.firstDate = firstDate;
	}

	public Long getPartnerId(Member member) {
		if (Objects.equals(this.getMember1().getId(), member.getId())) {
			return this.getMember2().getId();
		}
		return this.getMember1().getId();
	}
}
