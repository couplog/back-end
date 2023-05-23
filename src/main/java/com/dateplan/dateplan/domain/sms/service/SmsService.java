package com.dateplan.dateplan.domain.sms.service;

import com.dateplan.dateplan.domain.sms.type.SmsType;
import com.dateplan.dateplan.global.exception.sms.SmsSendFailException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

	private static final String DOMAIN = "https://api.coolsms.co.kr";
	private static final String PHONE_AUTH_TEXT = "[date-plan] 휴대전화 인증을 위한 인증 코드입니다. \n %s";
	private static final Set<String> SUCCESS_CODE = Set.of("2000", "3000", "4000");

	private final DefaultMessageService messageService;
	private final String sendNumber;

	public SmsService(@Value("${sms.key}") String key,
		@Value("${sms.secret}") String secret,
		@Value("${sms.send-number}") String sendNumber) {
		this.messageService = NurigoApp.INSTANCE.initialize(key, secret, DOMAIN);
		this.sendNumber = sendNumber;
	}

	public void sendSmsForPhoneAuthentication(String toNumber, int code) {

		Message message = createMessage(toNumber, code);

		SingleMessageSentResponse response = this.messageService.sendOne(
			new SingleMessageSendingRequest(message));

		boolean success = isSuccess(response);

		if (!success) {
			throw new SmsSendFailException(SmsType.PHONE_AUTHENTICATION);
		}
	}

	private boolean isSuccess(SingleMessageSentResponse response) {

		return response != null && SUCCESS_CODE.contains(response.getStatusCode());
	}

	private Message createMessage(String toNumber, int code) {

		Message message = new Message();

		message.setFrom(sendNumber);
		message.setTo(toNumber);
		message.setText(String.format(PHONE_AUTH_TEXT, code));

		return message;
	}
}
