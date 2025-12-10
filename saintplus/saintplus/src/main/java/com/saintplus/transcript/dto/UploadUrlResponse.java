package com.saintplus.transcript.dto;

import lombok.Builder;


@Builder
public record UploadUrlResponse(
        String uploadUrl, // 클라이언트가 파일을 보낼 임시 주소
        String fileKey    // S3에 저장될 최종 경로
) {}
