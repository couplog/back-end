package com.dateplan.dateplan.domain.member.service.dto.request;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConnectionServiceRequest {

	private String connectionCode;
	private LocalDate firstDate;

}
