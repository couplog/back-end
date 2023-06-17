package com.dateplan.dateplan.domain.anniversary.controller;

import com.dateplan.dateplan.domain.anniversary.dto.AnniversaryCreateRequest;
import com.dateplan.dateplan.domain.anniversary.service.AnniversaryService;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/couples/{couple_id}/anniversary")
public class AnniversaryController {

	private final AnniversaryService anniversaryService;

	@PostMapping
	public ApiResponse<Void> createAnniversary(@PathVariable("couple_id") Long coupleId,
		@RequestBody @Valid AnniversaryCreateRequest request) {

		anniversaryService.createAnniversaries(MemberThreadLocal.get(), coupleId, request.toServiceRequest());

		return ApiResponse.ofSuccess();
	}
}
