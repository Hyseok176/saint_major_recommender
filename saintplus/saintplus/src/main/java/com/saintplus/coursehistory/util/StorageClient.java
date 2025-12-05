package com.saintplus.coursehistory.util;

import com.saintplus.coursehistory.dto.UploadUrlResponse;

import java.io.InputStream;


public interface StorageClient {

    UploadUrlResponse generatePresignedUrl(String fileKey, String contentType);
    InputStream getObjectStream(String fileKey);

}
