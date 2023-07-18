package com.dateplan.dateplan.service;

import com.dateplan.dateplan.TestRedisConfig;
import com.dateplan.dateplan.domain.s3.S3Client;
import com.dateplan.dateplan.domain.sms.service.SmsSendClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class ServiceTestSupport {

	@MockBean
	protected SmsSendClient smsSendClient;

	@MockBean
	protected S3Client s3Client;
}
