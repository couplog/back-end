package com.dateplan.dateplan.domain.schedule.dto.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exists {

	private Boolean memberSchedule;
	private Boolean partnerSchedule;
	private Boolean dateSchedule;

	public void updateSchedule(boolean isPartner) {
		if (isPartner) {
			this.partnerSchedule = true;
		} else {
			this.memberSchedule = true;
		}
	}

	public static Exists getInstance() {
		return Exists.builder()
			.memberSchedule(false)
			.partnerSchedule(false)
			.dateSchedule(false)
			.build();
	}

}
