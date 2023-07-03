package com.dateplan.dateplan.domain.dating.service.dto.request;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.global.exception.schedule.InvalidDateTimeRangeException;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DatingCreateServiceRequest {

	private String title;
	private String location;
	private String content;
	private LocalDateTime startDateTime;
	private LocalDateTime endDateTime;

	public DatingCreateServiceRequest(
		String title,
		String location,
		String content,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime
	) {
		this.title = title;
		this.location = location;
		this.content = content;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		throwIfInvalidDateTimeRange();
	}

	private void throwIfInvalidDateTimeRange() {
		if (startDateTime.isAfter(endDateTime)) {
			throw new InvalidDateTimeRangeException();
		}
	}

	public Dating toDatingEntity(Couple couple) {
		return Dating.builder()
			.title(title)
			.location(location)
			.content(content)
			.startDateTime(startDateTime)
			.endDateTime(endDateTime)
			.couple(couple)
			.build();
	}
}
