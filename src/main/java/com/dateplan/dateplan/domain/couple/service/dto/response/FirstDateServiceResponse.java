package com.dateplan.dateplan.domain.couple.service.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FirstDateServiceResponse {

	private LocalDate firstDate;
}
