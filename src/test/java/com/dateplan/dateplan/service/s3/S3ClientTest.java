package com.dateplan.dateplan.service.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.dateplan.dateplan.domain.s3.S3Client;
import com.dateplan.dateplan.domain.s3.S3ImageType;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.S3Exception;
import com.dateplan.dateplan.global.exception.S3ImageNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3ClientTest {

	private final S3Client s3Client;
	private final AmazonS3 amazonS3;

	public S3ClientTest() {
		this.amazonS3 = mock(AmazonS3Client.class);
		this.s3Client = new S3Client("bucket", this.amazonS3);
	}

	@Nested
	@DisplayName("이미지 타입과 이름으로 S3 로 Presigned URL 요청시")
	class GetPreSignedUrl {

		@DisplayName("S3 로 정상적인 요청 및 응답이 온다면 URL 을 생성한다.")
		@Test
		void withAvailableS3() throws MalformedURLException {

			// Given
			String fileName = "fileName";
			S3ImageType imageType = S3ImageType.MEMBER_PROFILE;

			// Stub
			URL url = new URL("https://something.com");
			given(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
				.willReturn(url);

			// When & Then
			assertThatNoException().isThrownBy(
				() -> {
					URL preSignedUrl = s3Client.getPreSignedUrl(imageType, fileName);
					assertThat(preSignedUrl).isEqualTo(url);
				}
			);
		}

		@DisplayName("S3 로 요청 및 응답 과정에 문제가 있다면 예외를 발생시킨다.")
		@Test
		void withUnAvailableS3() {

			// Given
			String fileName = "fileName";
			S3ImageType imageType = S3ImageType.MEMBER_PROFILE;

			// Stub
			SdkClientException sdkClientException = new SdkClientException("message");
			given(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
				.willThrow(sdkClientException);

			// When & Then
			assertThatThrownBy(() -> s3Client.getPreSignedUrl(imageType, fileName))
				.isInstanceOf(S3Exception.class)
				.hasMessage(DetailMessage.S3_CREATE_PRESIGNED_URL_FAIL)
				.hasCauseInstanceOf(SdkClientException.class);
		}
	}

	@Nested
	@DisplayName("이미지 타입과 이름으로 S3 로 특정 이미지가 존재하는지 확인시")
	class ThrowIfImageNotFound {

		@DisplayName("S3 에 이미지가 존재한다면 예외를 발생시키지 않는다.")
		@Test
		void withExistsImage() {

			// Given
			String fileName = "fileName";
			S3ImageType imageType = S3ImageType.MEMBER_PROFILE;

			// Stub
			given(amazonS3.doesObjectExist(anyString(), anyString()))
				.willReturn(true);

			// When & Then
			assertThatNoException().isThrownBy(
				() -> s3Client.throwIfImageNotFound(imageType, fileName)
			);
		}

		@DisplayName("S3 에 이미지가 존재하지 않는다면 예외를 발생시킨다.")
		@Test
		void withNotExistsImage() {

			// Given
			String fileName = "fileName";
			S3ImageType imageType = S3ImageType.MEMBER_PROFILE;

			// Stub
			given(amazonS3.doesObjectExist(anyString(), anyString()))
				.willReturn(false);

			// When & Then
			assertThatThrownBy(() -> s3Client.throwIfImageNotFound(imageType, fileName))
				.isInstanceOf(S3ImageNotFoundException.class)
				.hasMessage(DetailMessage.S3_IMAGE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("이미지 타입과 이름으로 객체 URL을 요청하면")
	class GetObjectUrl {

		@DisplayName("S3 내에 해당 객체의 URL 를 반환한다.")
		@Test
		void withValid() throws MalformedURLException {

			// Given
			String fileName = "fileName";
			S3ImageType imageType = S3ImageType.MEMBER_PROFILE;

			// Stub
			URL expectedURL = new URL("https://something.com");
			given(amazonS3.getUrl(anyString(), anyString()))
				.willReturn(expectedURL);

			// When
			URL objectURL = s3Client.getObjectUrl(imageType, fileName);

			// Then
			assertThat(objectURL).isEqualTo(expectedURL);
		}
	}
}
