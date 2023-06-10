package com.dateplan.dateplan.controller.member.MemberController;

import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.ALREADY_CONNECTED;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CONNECTION_CODE;
import static com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage.INVALID_CONNECTION_CODE_PATTERN;
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
import com.dateplan.dateplan.global.constant.Gender;
import com.dateplan.dateplan.global.constant.Operation;
import com.dateplan.dateplan.global.constant.Resource;
import com.dateplan.dateplan.global.exception.ErrorCode;
import com.dateplan.dateplan.global.exception.NoPermissionException;
import com.dateplan.dateplan.global.exception.S3Exception;
import com.dateplan.dateplan.global.exception.S3ImageNotFoundException;
import com.dateplan.dateplan.global.exception.member.AlreadyConnectedException;
import com.dateplan.dateplan.global.exception.member.InvalidConnectionCodeException;
import com.dateplan.dateplan.global.exception.member.SelfConnectionNotAllowedException;
import com.dateplan.dateplan.global.util.RandomCodeGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
}
