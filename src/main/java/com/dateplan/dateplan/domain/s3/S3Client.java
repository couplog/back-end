package com.dateplan.dateplan.domain.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.dateplan.dateplan.global.exception.ErrorCode.DetailMessage;
import com.dateplan.dateplan.global.exception.S3Exception;
import java.net.URL;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Client {

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	private final AmazonS3 amazonS3;

	private static final int PRESIGNED_URL_EXPIRE_DURATION = 1000 * 60 * 5;

	public URL getPreSignedUrl(S3ImageType type, String fileName) {

		String fullPath = type.getFullPath(fileName);

		GeneratePresignedUrlRequest request = getGeneratePresignedUrlRequest(fullPath);

		try{
			return amazonS3.generatePresignedUrl(request);
		}catch (SdkClientException e) {

			throw new S3Exception(DetailMessage.S3_CREATE_PRESIGNED_URL_FAIL, e);
		}
	}

	private GeneratePresignedUrlRequest getGeneratePresignedUrlRequest(String fullPath) {

		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket,
			fullPath).withMethod(HttpMethod.PUT).withExpiration(getPreSignedUrlExpiration());

		request.addRequestParameter(Headers.S3_CANNED_ACL,
			CannedAccessControlList.PublicRead.toString());

		return request;
	}

	private Date getPreSignedUrlExpiration() {

		return new Date(new Date().getTime() + PRESIGNED_URL_EXPIRE_DURATION);
	}
}
