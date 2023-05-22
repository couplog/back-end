package com.dateplan.dateplan.domain.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LoginRequest {

	@NotNull
	private String phone;
	@NotNull
	private String password;
}
