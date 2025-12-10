package com.saintplus.transcript.util;

import com.saintplus.transcript.dto.UploadUrlResponse;

import java.io.InputStream;


public interface StorageClient {

    UploadUrlResponse generatePresignedUrl(String fileKey, String contentType);
    InputStream getObjectStream(String fileKey);

}
