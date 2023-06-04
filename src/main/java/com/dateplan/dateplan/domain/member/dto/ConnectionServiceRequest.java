package com.dateplan.dateplan.domain.member.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConnectionServiceRequest {

	private String connectionCode;
	private LocalDate firstDate;

}
