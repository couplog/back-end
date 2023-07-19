package com.dateplan.dateplan.domain.member.service.dto.request;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CheckPasswordServiceRequest {

	private String password;
}
