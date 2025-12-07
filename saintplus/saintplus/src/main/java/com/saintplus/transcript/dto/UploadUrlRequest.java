package com.saintplus.transcript.dto;

import lombok.Builder;


@Builder
public record UploadUrlRequest(
        String filename,  // "https://..."
        String contentType  // 추가
) {}
