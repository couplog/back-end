package com.dateplan.dateplan.global.health.controller;

import com.dateplan.dateplan.global.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

	@GetMapping("/health")
	public ApiResponse<Void> healthCheck(){

		return ApiResponse.ofSuccess();
	}
}
