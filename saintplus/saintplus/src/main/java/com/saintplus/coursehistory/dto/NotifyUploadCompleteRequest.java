package com.saintplus.coursehistory.dto;

public record NotifyUploadCompleteRequest(
        String fileKey,
        String major1,
        String major2,
        String major3
) {
}
