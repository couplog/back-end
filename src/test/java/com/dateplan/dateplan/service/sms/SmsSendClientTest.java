package com.dateplan.dateplan.service.sms;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.dateplan.dateplan.domain.sms.service.SmsSendClient;
import com.dateplan.dateplan.domain.sms.type.SmsType;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.sms.SmsSendFailException;
import net.nurigo.sdk.message.model.MessageType;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmsSendClientTest {

	private final SmsSendClient smsSendClient;

	private final DefaultMessageService defaultMessageService;

	public SmsSendClientTest() {
		defaultMessageService = mock(DefaultMessageService.class);
		this.smsSendClient = new SmsSendClient(defaultMessageService, "01012341234");
	}

	@DisplayName("sms 발송시 성공 코드를 반환받는다면 예외를 발생시키지 않는다.")
	@CsvSource({"2000", "3000", "4000"})
	@ParameterizedTest
	void sendSmsIfReceiveSuccessCode(String statusCode) {

		// Given
		int code = 123456;
		String toNumber = "01012345678";
		SingleMessageSentResponse singleMessageSentResponse = createMessageResponse(statusCode);

		given(defaultMessageService.sendOne(any(SingleMessageSendingRequest.class)))
			.willReturn(singleMessageSentResponse);

		// When & Then
		assertThatNoException().isThrownBy(
			() -> smsSendClient.sendSmsForPhoneAuthentication(toNumber, code));
	}

	@DisplayName("sms 발송시 실패 코드를 반환받는다면 예외를 발생시킨다.")
	@CsvSource({"2001", "3001", "4001"})
	@ParameterizedTest
	void sendSmsIfReceiveFailCode(String statusCode) {

		// Given
		int code = 123456;
		String toNumber = "01012345678";
		SingleMessageSentResponse singleMessageSentResponse = createMessageResponse(statusCode);

		given(defaultMessageService.sendOne(any(SingleMessageSendingRequest.class)))
			.willReturn(singleMessageSentResponse);

		// When & Then
		assertThatThrownBy(() -> smsSendClient.sendSmsForPhoneAuthentication(toNumber, code))
			.isInstanceOf(SmsSendFailException.class)
			.hasMessage(String.format(DetailMessage.SMS_SEND_FAIL, SmsType.PHONE_AUTHENTICATION.getName()));
	}

	private SingleMessageSentResponse createMessageResponse(String statusCode) {

		return new SingleMessageSentResponse(
			"groupId",
			"to",
			"from",
			MessageType.SMS,
			"message",
			"country",
			"messageId",
			statusCode,
			"accountId"
		);
	}
}
