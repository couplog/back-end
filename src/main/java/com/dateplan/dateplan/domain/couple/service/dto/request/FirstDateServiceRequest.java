package com.dateplan.dateplan.domain.couple.service.dto.request;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FirstDateServiceRequest {

	LocalDate firstDate;
}
