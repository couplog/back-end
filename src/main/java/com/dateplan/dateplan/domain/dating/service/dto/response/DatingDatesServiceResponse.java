package com.dateplan.dateplan.domain.dating.service.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DatingDatesServiceResponse {

	List<LocalDate> datingDates;
}
