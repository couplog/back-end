package com.dateplan.dateplan.domain.anniversary.interceptor;

import com.dateplan.dateplan.domain.anniversary.repository.AnniversaryQueryRepository;
import com.dateplan.dateplan.domain.couple.repository.CoupleQueryRepository;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
public class AnniversaryAuthInterceptor implements HandlerInterceptor {

	private final CoupleQueryRepository coupleQueryRepository;
	private final AnniversaryQueryRepository anniversaryQueryRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
		Object handler) throws Exception {

		Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(
			HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		String method = request.getMethod();

		Member member = MemberThreadLocal.get();

		String coupleIdStr = pathVariables.get("couple_id");
		String anniversaryIdStr = pathVariables.get("anniversary_id");

		if (coupleIdStr != null) {
			Long coupleId = Long.valueOf(coupleIdStr);

			if (!coupleQueryRepository.existsByIdAndMemberId(coupleId, member.getId())) {
				throwNoPermissionException(method);
			}

			if (anniversaryIdStr != null) {
				Long anniversaryId = Long.valueOf(anniversaryIdStr);

				if (!anniversaryQueryRepository.existsByIdAndCoupleId(anniversaryId, coupleId)) {
					throwNoPermissionException(method);
				}
			}
		}

		return true;
	}

	public void throwNoPermissionException(String method) {

		Operation operation = switch (method) {
			case "GET" -> Operation.READ;
			case "POST" -> Operation.CREATE;
			case "DELETE" -> Operation.DELETE;
			default -> Operation.UPDATE;
		};

		throw new NoPermissionException(Resource.ANNIVERSARY, operation);
	}
}
