package com.dateplan.dateplan.domain.dating.controller;

import com.dateplan.dateplan.domain.dating.controller.dto.request.DatingCreateRequest;
import com.dateplan.dateplan.domain.dating.service.DatingService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/couples")
public class DatingController {

	private final DatingService datingService;

	@ResponseStatus(value = HttpStatus.CREATED)
	@PostMapping("/{couple_id}/dating")
	public ApiResponse<Void> createDating(
		@PathVariable("couple_id") Long coupleId,
		@Valid @RequestBody DatingCreateRequest request
	) {
		final Member member = MemberThreadLocal.get();
		datingService.createDating(member, coupleId, request.toDatingCreateServiceRequest());
		return ApiResponse.ofSuccess();
	}

}
