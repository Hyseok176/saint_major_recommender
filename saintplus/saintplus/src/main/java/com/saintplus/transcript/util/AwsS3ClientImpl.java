package com.saintplus.transcript.util;

import com.saintplus.transcript.dto.UploadUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.InputStream;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AwsS3ClientImpl implements StorageClient {

    private final S3Presigner presigner;
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;


    @Override
    public UploadUrlResponse generatePresignedUrl(String fileKey, String contentType) {

        // 1. S3에 PUT 요청을 보낼 때 사용할 요청 객체 정의
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(this.bucketName)
                .key(fileKey)
                .contentType(contentType)
                .build();

        // 2. Presigned URL 생성 요청
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r -> r
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(10))); // 10분 동안 유효한 URL

        // 3. 응답 DTO에 담아 반환
        return UploadUrlResponse.builder()
                .uploadUrl(presignedRequest.url().toString())
                .fileKey(fileKey)
                .build();

    }


    @Override
    public InputStream getObjectStream(String fileKey) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(this.bucketName)
                .key(fileKey)
                .build();

        try {
            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            throw new IllegalStateException("S3 object not found: " + fileKey, e);
        } catch (SdkException e) {
            throw new IllegalStateException("Failed to read S3 object: " + fileKey, e);
        }

    }

}
