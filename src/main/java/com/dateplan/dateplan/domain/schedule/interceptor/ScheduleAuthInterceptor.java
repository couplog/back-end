package com.dateplan.dateplan.domain.schedule.interceptor;

import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.entity.Schedule;
import com.dateplan.dateplan.domain.schedule.service.ScheduleReadService;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
public class ScheduleAuthInterceptor implements HandlerInterceptor {

	private final ScheduleReadService scheduleReadService;
	private final CoupleReadService coupleReadService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
		Object handler) throws Exception {
		Map<String, String> map = (Map<String, String>) request.getAttribute(
			HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		String method = request.getMethod();

		Member member = MemberThreadLocal.get();
		String memberId = map.get("member_id");
		String scheduleId = map.get("schedule_id");

		if (memberId != null) {
			if (method.equals("GET")) {
				Long partnerId = coupleReadService.getPartnerId(member);
				if (isNotSameMember(Long.valueOf(memberId), member.getId())
					&& isNotSameMember(Long.valueOf(memberId), partnerId)) {
					throwNoPermissionException(method);
				}
			} else {
				if (isNotSameMember(Long.valueOf(memberId), member.getId())) {
					throwNoPermissionException(method);
				}
			}
			if (scheduleId != null) {
				Schedule schedule = scheduleReadService.findScheduleByIdOrElseThrow(
					Long.valueOf(scheduleId));
				if (isNotScheduleOwner(Long.valueOf(memberId), schedule)) {
					throwNoPermissionException(method);
				}
			}
		}

		return true;
	}

	private boolean isNotSameMember(Long requestId, Long memberId) {
		return !Objects.equals(requestId, memberId);
	}

	private boolean isNotScheduleOwner(Long memberId, Schedule schedule) {
		return !Objects.equals(memberId, schedule.getSchedulePattern().getMember().getId());
	}

	private void throwNoPermissionException(String method) {
		Operation operation = switch (method) {
			case "GET" -> Operation.READ;
			case "POST" -> Operation.CREATE;
			case "PUT" -> Operation.UPDATE;
			default -> Operation.DELETE;
		};
		throw new NoPermissionException(Resource.SCHEDULE, operation);
	}
}
