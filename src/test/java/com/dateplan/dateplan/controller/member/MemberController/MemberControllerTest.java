package com.dateplan.dateplan.controller.member.MemberController;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_CONNECTED;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CONNECTION_CODE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CONNECTION_CODE_PATTERN;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DATE_TIME_RANGE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_DIFFERENCE_DATE_TIME;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_REPEAT_END_TIME_RANGE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_REPEAT_RULE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_LOCATION;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_SCHEDULE_TITLE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.S3_CREATE_PRESIGNED_URL_FAIL;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.S3_DELETE_OBJECT_FAIL;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.S3_IMAGE_NOT_FOUND;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.SELF_CONNECTION_NOT_ALLOWED;
import static com.dateplan.dateplan.global.exception.ErrorCode.INVALID_INPUT_VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amazonaws.SdkClientException;
import com.dateplan.dateplan.controller.ControllerTestSupport;
import com.dateplan.dateplan.domain.member.dto.ConnectionRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceRequest;
import com.dateplan.dateplan.domain.member.dto.ConnectionServiceResponse;
import com.dateplan.dateplan.domain.member.dto.MemberInfoServiceResponse;
import com.dateplan.dateplan.domain.member.dto.PresignedURLResponse;
import com.dateplan.dateplan.domain.member.dto.ProfileImageURLServiceResponse;
import com.dateplan.dateplan.domain.member.entity.Member;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleRequest;
import com.dateplan.dateplan.domain.schedule.dto.ScheduleServiceRequest;
import com.dateplan.dateplan.global.auth.MemberThreadLocal;
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.RepeatRule;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.S3Exception;
import com.dateplan.dateplan.global.exception.S3ImageNotFoundException;
import com.dateplan.dateplan.global.exception.auth.NoPermissionException;
import com.dateplan.dateplan.global.exception.couple.MemberNotConnectedException;
import com.dateplan.dateplan.global.exception.member.AlreadyConnectedException;
import com.dateplan.dateplan.global.exception.member.InvalidConnectionCodeException;
import com.dateplan.dateplan.global.exception.member.SelfConnectionNotAllowedException;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.http.MediaType;

public class MemberControllerTest extends ControllerTestSupport {

	@BeforeEach
	void setUp() throws Exception {
		given(
			authInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class),
				any(Object.class)))
			.willReturn(true);
	}

	@Nested
	@DisplayName("회원 연결 코드 조회 시")
	class GetConnectionCode {

		private static final String REQUEST_URL = "/api/members/{member_id}/connect";

		@DisplayName("자신의 id가 아닌 다른 id를 요청하면 실패한다.")
		@Test
		void failWithoutPermission() throws Exception {

			// Given
			NoPermissionException exception =
				new NoPermissionException(Resource.MEMBER, Operation.READ);

			//Stub
			given(coupleService.getConnectionCode(anyLong()))
				.willThrow(exception);

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL, "1"))
				.andExpect(status().isForbidden())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.NO_PERMISSION.getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}

		@DisplayName("생성한 코드 또는 24시간 내에 생성된 코드를 반환한다")
		@Test
		void returnConnectionCode() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionServiceResponse response = createConnectionServiceResponse(connectionCode);

			// Stub
			given(coupleService.getConnectionCode(anyLong()))
				.willReturn(response);

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL, "1"))
				.andExpect(status().isOk())
				.andExpectAll(
					jsonPath("$.success").value("true"),
					jsonPath("$.data.connectionCode").value(connectionCode)
				);
		}

	}

	@Nested
	@DisplayName("회원 연결 시")
	class ConnectCouple {

		private static final String REQUEST_URL = "/api/members/{member_id}/connect";

		@DisplayName("자신의 id가 아닌 다른 id를 요청하면 실패한다.")
		@Test
		void failWithoutPermission() throws Exception {

			// Given
			NoPermissionException exception =
				new NoPermissionException(Resource.MEMBER, Operation.UPDATE);

			//Stub
			given(coupleService.getConnectionCode(anyLong()))
				.willThrow(exception);

			// When & Then
			mockMvc.perform(
					get(REQUEST_URL, "1"))
				.andExpect(status().isForbidden())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.NO_PERMISSION.getCode()),
					jsonPath("$.message").value(exception.getMessage())
				);
		}

		@DisplayName("상대의 올바른 연결 코드를 입력하면 커플 생성에 성공한다.")
		@Test
		void successConnectCouple() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willDoNothing()
				.given(coupleService)
				.connectCouple(anyLong(), any(ConnectionServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL, "1")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("올바르지 않은 패턴의 연결 코드를 입력하면 실패한다")
		@CsvSource({"12345", "123abc", "abcde", "패턴"})
		@NullAndEmptySource
		@ParameterizedTest
		void connectCoupleWithInvalidConnectionCodePattern(String connectionCode) throws Exception {

			// Given
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willDoNothing()
				.given(coupleService)
				.connectCouple(anyLong(), any(ConnectionServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL, "1")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()),
					jsonPath("$.message").value(INVALID_CONNECTION_CODE_PATTERN)
				);
		}

		@DisplayName("존재하지 않은 연결 코드를 입력하면 실패한다")
		@Test
		void connectCoupleWithNotFoundCode() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willThrow(new InvalidConnectionCodeException())
				.given(coupleService)
				.connectCouple(anyLong(), any(ConnectionServiceRequest.class));

			mockMvc.perform(
					post(REQUEST_URL, "1")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.INVALID_CONNECTION_CODE.getCode()),
					jsonPath("$.message").value(INVALID_CONNECTION_CODE)
				);
		}

		@DisplayName("상대방이 이미 연결되어있다면 실패한다")
		@Test
		void connectCoupleWithAlreadyConnected() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willThrow(new AlreadyConnectedException())
				.given(coupleService)
				.connectCouple(anyLong(), any(ConnectionServiceRequest.class));

			// When & Then
			mockMvc.perform(
					post(REQUEST_URL, "1")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isConflict())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.ALREADY_CONNECTED.getCode()),
					jsonPath("$.message").value(ALREADY_CONNECTED)
				);
		}

		@DisplayName("자기 자신과 연결하려 하면 실패한다")
		@Test
		void connectCoupleWithSelf() throws Exception {

			// Given
			String connectionCode = RandomCodeGenerator.generateConnectionCode(6);
			ConnectionRequest request = createConnectionRequest(connectionCode);

			// Stub
			willThrow(new SelfConnectionNotAllowedException())
				.given(coupleService)
				.connectCouple(anyLong(), any(ConnectionServiceRequest.class));

			mockMvc.perform(
					post(REQUEST_URL, "1")
						.content(om.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
						.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.SELF_CONNECTION_NOT_ALLOWED.getCode()),
					jsonPath("$.message").value(SELF_CONNECTION_NOT_ALLOWED)
				);
		}

	}

	@Nested
	@DisplayName("프로필 이미지 업로드를 위한 Presigned URL 요청시")
	class GetPresignedURL {

		private static final String REQUEST_URL = "/api/members/{member_id}/profile/image/presigned-url";

		@DisplayName("S3 요청, 응답에 문제가 없다면 Presigned URL 을 응답한다.")
		@Test
		void withAvailableS3() throws Exception {

			// Given
			String expectedURLStr = "https://something.com";
			PresignedURLResponse expectedResponse = createPresignedURLResponse(expectedURLStr);

			// Stub
			given(memberService.getPresignedURLForProfileImage(anyLong()))
				.willReturn(expectedResponse);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.presignedURL").value(expectedURLStr));
		}

		@DisplayName("S3 요청, 응답에 문제가 있다면 에러 코드, 메시지를 응답한다.")
		@Test
		void withUnAvailableS3() throws Exception {

			// Stub
			SdkClientException sdkClientException = new SdkClientException("message");
			S3Exception expectedException = new S3Exception(S3_CREATE_PRESIGNED_URL_FAIL,
				sdkClientException);
			given(memberService.getPresignedURLForProfileImage(anyLong()))
				.willThrow(expectedException);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L))
				.andExpect(status().isServiceUnavailable())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.S3_ERROR.getCode()),
					jsonPath("$.message").value(S3_CREATE_PRESIGNED_URL_FAIL)
				);
		}

		@DisplayName("현재 로그인한 회원과 Presigned URL 조회 대상 회원이 다르다면 에러 코드, 메시지를 응답한다.")
		@Test
		void withNoPermission() throws Exception {

			// Stub
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.READ);
			given(memberService.getPresignedURLForProfileImage(anyLong()))
				.willThrow(expectedException);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L))
				.andExpect(status().isForbidden())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.NO_PERMISSION.getCode()),
					jsonPath("$.message").value(expectedException.getMessage())
				);
		}
	}

	@Nested
	@DisplayName("S3 내에 있는 프로필 사진 URL 을 DB 에 저장하려고 할 때")
	class CheckAndSaveImage {

		private static final String REQUEST_URL = "/api/members/{member_id}/profile/image";

		@DisplayName("S3 내에 해당 유저의 프로필 이미지가 존재한다면, 요청에 성공한다.")
		@Test
		void withExistsProfileImageInS3() throws Exception {

			// Stub
			willDoNothing()
				.given(memberService)
				.checkAndSaveProfileImage(anyLong());

			// When & Then
			mockMvc.perform(put(REQUEST_URL, 1L))
				.andExpect(status().isOk());
		}

		@DisplayName("S3 내에 해당 유저의 프로필 이미지가 존재하지 않는다면, 에러 코드, 메시지를 응답한다.")
		@Test
		void withNotExistsProfileImageInS3() throws Exception {

			// Stub
			S3ImageNotFoundException expectedException = new S3ImageNotFoundException();
			willThrow(expectedException)
				.given(memberService)
				.checkAndSaveProfileImage(anyLong());

			// When & Then
			mockMvc.perform(put(REQUEST_URL, 1L))
				.andExpect(status().isConflict())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.S3_IMAGE_NOT_FOUND.getCode()),
					jsonPath("$.message").value(S3_IMAGE_NOT_FOUND)
				);
		}

		@DisplayName("현재 로그인한 회원과 프로필 이미지 수정 대상 회원이 다르다면 에러 코드, 메시지를 응답한다.")
		@Test
		void withNoPermission() throws Exception {

			// Stub
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.UPDATE);
			willThrow(expectedException)
				.given(memberService)
				.checkAndSaveProfileImage(anyLong());

			// When & Then
			mockMvc.perform(put(REQUEST_URL, 1L))
				.andExpect(status().isForbidden())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.NO_PERMISSION.getCode()),
					jsonPath("$.message").value(expectedException.getMessage())
				);
		}
	}

	@Nested
	@DisplayName("회원 프로필 이미지 삭제 요청시")
	class DeleteProfileImage {

		private static final String REQUEST_URL = "/api/members/{member_id}/profile/image";

		@DisplayName("S3 요청, 응답에 문제가 없다면 요청에 성공한다.")
		@Test
		void withAvailableS3() throws Exception {

			// Stub
			willDoNothing()
				.given(memberService)
				.deleteProfileImage(anyLong());

			// When & Then
			mockMvc.perform(delete(REQUEST_URL, 1L))
				.andExpect(status().isOk());
		}

		@DisplayName("S3 요청, 응답에 문제가 있다면 에러 코드, 메시지를 응답한다.")
		@Test
		void withUnAvailableS3() throws Exception {

			// Stub
			SdkClientException sdkClientException = new SdkClientException("message");
			S3Exception expectedException = new S3Exception(S3_DELETE_OBJECT_FAIL,
				sdkClientException);
			willThrow(expectedException)
				.given(memberService)
				.deleteProfileImage(anyLong());

			// When & Then
			mockMvc.perform(delete(REQUEST_URL, 1L))
				.andExpect(status().isServiceUnavailable())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.S3_ERROR.getCode()),
					jsonPath("$.message").value(S3_DELETE_OBJECT_FAIL)
				);
		}

		@DisplayName("현재 로그인한 회원과 프로필 이미지 삭제 대상 회원이 다르다면 에러 코드, 메시지를 응답한다.")
		@Test
		void withNoPermission() throws Exception {

			// Stub
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.DELETE);
			willThrow(expectedException)
				.given(memberService)
				.deleteProfileImage(anyLong());

			// When & Then
			mockMvc.perform(delete(REQUEST_URL, 1L))
				.andExpect(status().isForbidden())
				.andExpectAll(
					jsonPath("$.success").value("false"),
					jsonPath("$.code").value(ErrorCode.NO_PERMISSION.getCode()),
					jsonPath("$.message").value(expectedException.getMessage())
				);
		}
	}

	@Nested
	@DisplayName("현재 로그인 회원의 정보 조회 요청시")
	class GetCurrentLoginMemberInfo {

		private static final String REQUEST_URL = "/api/members/me";

		@DisplayName("현재 로그인 회원의 정보를 응답한다.")
		@Test
		void withLoginMember() throws Exception {

			// Stub
			MemberInfoServiceResponse serviceResponse = createMemberInfoServiceResponse();
			boolean isConnected = true;
			given(memberReadService.getCurrentLoginMemberInfo())
				.willReturn(serviceResponse);
			given(coupleReadService.isCurrentLoginMemberConnected())
				.willReturn(isConnected);

			// When & Then
			mockMvc.perform(get(REQUEST_URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpectAll(
					jsonPath("$.data.memberId").value(serviceResponse.getMemberId()),
					jsonPath("$.data.name").value(serviceResponse.getName()),
					jsonPath("$.data.nickname").value(serviceResponse.getNickname()),
					jsonPath("$.data.phone").value(serviceResponse.getPhone()),
					jsonPath("$.data.birth").value(serviceResponse.getBirth().toString()),
					jsonPath("$.data.gender").value(serviceResponse.getGender().name()),
					jsonPath("$.data.profileImageURL").value(serviceResponse.getProfileImageURL()));
		}
	}

	@Nested
	@DisplayName("특정 회원의 프로필 이미지 조회 요청시")
	class GetProfileImageURL {

		private static final String REQUEST_URL = "/api/members/{member_id}/profile/image";

		@DisplayName("로그인한 회원 자신 혹은 연결된 회원의 프로필 이미지 조회 요청이면 성공한다.")
		@Test
		void withLoginMemberIdOrPartnerMemberId() throws Exception {

			// Stub
			Member loginMember = createMember();
			MemberThreadLocal.set(loginMember);
			Long partnerId = 2L;
			ProfileImageURLServiceResponse serviceResponse = createProfileImageURLServiceResponse();
			given(coupleReadService.getPartnerId(any(Member.class)))
				.willReturn(partnerId);
			given(memberReadService.getProfileImageURL(anyLong(), anyLong()))
				.willReturn(serviceResponse);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"))
				.andExpect(
					jsonPath("$.data.profileImageURL").value(serviceResponse.getProfileImageURL()));
		}

		@DisplayName("다른 회원과 연결되지 않은 회원의 요청이면 에러 코드, 메시지를 응답한다.")
		@Test
		void withNotConnectedMember() throws Exception {

			// Stub
			Member loginMember = createMember();
			MemberThreadLocal.set(loginMember);
			MemberNotConnectedException expectedException = new MemberNotConnectedException();
			given(coupleReadService.getPartnerId(any(Member.class)))
				.willThrow(expectedException);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(expectedException.getErrorCode().getCode()))
				.andExpect(jsonPath("$.message").value(expectedException.getMessage()));
		}

		@DisplayName("로그인한 회원 및 연결된 회원에 대한 요청이 아니면 에러 코드, 메시지를 응답한다.")
		@Test
		void withNotLoginMemberIdAndPartnerMemberId() throws Exception {

			// Stub
			Member loginMember = createMember();
			MemberThreadLocal.set(loginMember);

			Long partnerId = 2L;
			given(coupleReadService.getPartnerId(any(Member.class)))
				.willReturn(partnerId);

			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.READ);
			given(memberReadService.getProfileImageURL(anyLong(), anyLong()))
				.willThrow(expectedException);

			// When & Then
			mockMvc.perform(get(REQUEST_URL, 1L))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(expectedException.getErrorCode().getCode()))
				.andExpect(jsonPath("$.message").value(expectedException.getMessage()));
		}
	}

	@Nested
	@DisplayName("개인 일정을 생성할 때")
	class CreateSchedule {

		private final static String REQUEST_URL = "/api/members/{member_id}/schedules";

		@DisplayName("올바른 멤버 id와, 일정 정보를 입력하면 성공한다.")
		@Test
		void successWithValidRequest() throws Exception {

			// Given
			ScheduleRequest request = createScheduleRequest();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value("true"));
		}

		@DisplayName("로그인한 회원에 대한 요청이 아니면 실패한다")
		@Test
		void failWithNoPermissionRequest() throws Exception {

			// Given
			ScheduleRequest request = createScheduleRequest();

			// Stub
			NoPermissionException expectedException = new NoPermissionException(Resource.MEMBER,
				Operation.READ);
			willThrow(expectedException)
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(expectedException.getErrorCode().getCode()))
				.andExpect(jsonPath("$.message").value(expectedException.getMessage()));
		}

		@DisplayName("반복 종료일자가 유효하지 않으면 실패한다.")
		@ParameterizedTest
		@CsvSource({"2050-01-01", "2010-01-01"})
		void failWithInvalidRepeatEndTime(LocalDate repeatEndDate) throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().plusDays(5))
				.location("location")
				.content("content")
				.repeatRule(RepeatRule.M)
				.repeatEndTime(repeatEndDate)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(
					jsonPath("$.code").value(ErrorCode.INVALID_REPEAT_END_TIME_RANGE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_REPEAT_END_TIME_RANGE));
		}

		@DisplayName("날짜 간격이 유효하지 않으면 실패한다")
		@CsvSource({"2023-01-01T15:00, 2023-01-02T16:00, D",
			"2023-01-01T15:00, 2023-01-09T16:00, W",
			"2023-01-01T15:00, 2023-02-02T16:00, M",
			"2023-01-01T15:00, 2024-01-02T16:00, Y",})
		@ParameterizedTest
		void failWithInvalidDifferenceDateTime(LocalDateTime startDateTime,
			LocalDateTime endDateTime,
			RepeatRule repeatRule) throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(startDateTime)
				.endDateTime(endDateTime)
				.repeatRule(repeatRule)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(
					jsonPath("$.code").value(ErrorCode.INVALID_DIFFERENCE_DATE_TIME.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_DIFFERENCE_DATE_TIME));
		}

		@DisplayName("일정 종료일자가 일정 시작일자보다 앞서면 실패한다")
		@Test
		void failWithInvalidDateTimeRange() throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().minusDays(1))
				.location("location")
				.content("content")
				.repeatRule(RepeatRule.M)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(ErrorCode.INVALID_DATE_TIME_RANGE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_DATE_TIME_RANGE));
		}

		@DisplayName("올바르지 않은 제목을 입력하면 실패한다.")
		@Test
		void failWithInvalidTitle() throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title(createRandomString(20))
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().minusDays(1))
				.location("location")
				.content("content")
				.repeatRule(RepeatRule.M)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_SCHEDULE_TITLE));
		}

		@DisplayName("올바르지 않은 지역을 입력하면 실패한다.")
		@Test
		void failWithInvalidLocation() throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().minusDays(1))
				.location(createRandomString(30))
				.content("content")
				.repeatRule(RepeatRule.M)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_SCHEDULE_LOCATION));
		}

		@DisplayName("올바르지 않은 일정 내용을 입력하면 실패한다.")
		@Test
		void failWithInvalidContent() throws Exception {

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().minusDays(1))
				.location(createRandomString(110))
				.content("content")
				.repeatRule(RepeatRule.M)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_SCHEDULE_LOCATION));
		}

		@DisplayName("올바르지 않은 반복 패턴을 입력하면 실패한다.")
		@ParameterizedTest
		@CsvSource({"A", "B", "NDWMY", "가나다", "1", "!@"})
		void failWithInvalidRepeatRule(String rule) throws Exception {

			RepeatRule repeatRule = RepeatRule.from(rule);

			// Given
			ScheduleRequest request = ScheduleRequest.builder()
				.title("title")
				.startDateTime(LocalDateTime.now())
				.endDateTime(LocalDateTime.now().minusDays(1))
				.repeatRule(repeatRule)
				.build();

			// Stub
			willDoNothing()
				.given(scheduleService)
				.createSchedule(anyLong(), any(ScheduleServiceRequest.class));

			// When & Then
			mockMvc.perform(post(REQUEST_URL, 1L)
					.content(om.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
					.characterEncoding(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.code").value(INVALID_INPUT_VALUE.getCode()))
				.andExpect(jsonPath("$.message").value(INVALID_REPEAT_RULE));
		}
	}

	private ConnectionServiceResponse createConnectionServiceResponse(String connectionCode) {
		return ConnectionServiceResponse.builder()
			.connectionCode(connectionCode)
			.build();
	}

	private ConnectionRequest createConnectionRequest(String connectionCode) {
		return ConnectionRequest.builder()
			.connectionCode(connectionCode)
			.firstDate(LocalDate.now().minusDays(1L))
			.build();
	}

	private PresignedURLResponse createPresignedURLResponse(String presignedURL) {

		return PresignedURLResponse.builder()
			.presignedURL(presignedURL)
			.build();
	}

	private MemberInfoServiceResponse createMemberInfoServiceResponse() {

		return MemberInfoServiceResponse.builder()
			.memberId(1L)
			.name("횽길동")
			.nickname("nickname")
			.phone("01012341234")
			.birth(LocalDate.of(2020, 10, 10))
			.gender(Gender.MALE)
			.profileImageURL("imageURL")
			.build();
	}

	private ProfileImageURLServiceResponse createProfileImageURLServiceResponse() {

		return ProfileImageURLServiceResponse.builder()
			.profileImageURL("profileImageURL")
			.build();
	}

	private Member createMember() {

		return Member.builder()
			.name("홍길동")
			.nickname("nickname")
			.phone("01012341234")
			.password("password")
			.gender(Gender.FEMALE)
			.build();
	}

	private ScheduleRequest createScheduleRequest() {
		return ScheduleRequest.builder()
			.title("title")
			.startDateTime(LocalDateTime.now())
			.endDateTime(LocalDateTime.now().plusDays(5))
			.location("location")
			.content("content")
			.repeatRule(RepeatRule.M)
			.repeatEndTime(LocalDate.now().plusYears(10))
			.build();
	}

	private String createRandomString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			char randomChar = (char) ('A' + (int) (Math.random() * 26));
			sb.append(randomChar);
		}
		return sb.toString();
	}
}
