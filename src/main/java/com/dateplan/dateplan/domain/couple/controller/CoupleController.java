package com.dateplan.dateplan.domain.couple.controller;

import com.dateplan.dateplan.domain.couple.dto.FirstDateRequest;
import com.dateplan.dateplan.domain.couple.dto.FirstDateResponse;
import com.dateplan.dateplan.domain.couple.service.CoupleService;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/couple")
public class CoupleController {

	private final CoupleService coupleService;

	@GetMapping("/first-date")
	public ApiResponse<FirstDateResponse> getFirstDate() {
		FirstDateResponse response = coupleService.getFirstDate().toFirstDateResponse();
		return ApiResponse.ofSuccess(response);
	}

	@PutMapping("/first-date")
	public ApiResponse<Void> updateFirstDate(@Valid @RequestBody FirstDateRequest request) {
		coupleService.updateFirstDate(request.toFirstDateServiceRequest());
		return ApiResponse.ofSuccess();
	}
}
