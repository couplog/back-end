package com.dateplan.dateplan.domain.dating.controller.dto.response;

import com.dateplan.dateplan.domain.dating.entity.Dating;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DatingEntry {

	private Long datingId;
	private String title;
	private String location;
	private String content;
	private LocalDateTime startDateTime;
	private LocalDateTime endDateTime;

	public static DatingEntry from(Dating dating) {
		return DatingEntry.builder()
			.datingId(dating.getId())
			.title(dating.getTitle())
			.location(dating.getLocation())
			.content(dating.getContent())
			.startDateTime(dating.getStartDateTime())
			.endDateTime(dating.getEndDateTime())
			.build();
	}
}
