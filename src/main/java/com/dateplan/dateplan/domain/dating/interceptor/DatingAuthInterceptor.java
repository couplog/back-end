package com.dateplan.dateplan.domain.dating.interceptor;

import com.dateplan.dateplan.domain.couple.entity.Couple;
import com.dateplan.dateplan.domain.couple.service.CoupleReadService;
import com.dateplan.dateplan.domain.dating.entity.Dating;
import com.dateplan.dateplan.domain.dating.service.DatingReadService;
import com.dateplan.dateplan.domain.member.entity.Member;
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
public class DatingAuthInterceptor implements HandlerInterceptor {

	private final CoupleReadService coupleReadService;
	private final DatingReadService datingReadService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
		Object handler) throws Exception {
		Map<String, String> map = (Map<String, String>) request.getAttribute(
			HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		String method = request.getMethod();

		Member member = MemberThreadLocal.get();
		String coupleId = map.get("couple_id");
		String datingId = map.get("dating_id");
		Couple couple = coupleReadService.findCoupleByMemberOrElseThrow(member);

		if (coupleId != null) {
			if (isNotSameCouple(Long.valueOf(coupleId), couple.getId())) {
				throwNoPermissionException(method);
			}
			if (datingId != null) {
				Dating dating = datingReadService.findByDatingId(Long.valueOf(datingId));
				if (isNotDatingOwner(Long.valueOf(coupleId), dating.getCouple().getId())) {
					throwNoPermissionException(method);
				}
			}
		}

		return true;
	}

	private boolean isNotSameCouple(Long requestId, Long coupleId) {
		return !Objects.equals(requestId, coupleId);
	}

	private boolean isNotDatingOwner(Long coupleId, Long datingOwnerId) {
		return !Objects.equals(coupleId, datingOwnerId);
	}

	private void throwNoPermissionException(String method) {
		Operation operation = switch (method) {
			case "GET" -> Operation.READ;
			case "POST" -> Operation.CREATE;
			case "PUT" -> Operation.UPDATE;
			default -> Operation.DELETE;
		};
		throw new NoPermissionException(Resource.DATING, operation);
	}
}
