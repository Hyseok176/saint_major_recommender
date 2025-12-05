package com.saintplus.coursehistory.controller;

import com.saintplus.common.security.UserPrincipal;
import com.saintplus.coursehistory.dto.NotifyUploadCompleteRequest;
import com.saintplus.coursehistory.dto.UploadUrlRequest;
import com.saintplus.coursehistory.dto.UploadUrlResponse;
import com.saintplus.coursehistory.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/v1/transcripts")
@RequiredArgsConstructor
public class TranscriptController {

    private final TranscriptService transcriptService;


    // 전공 먼저 추출
    @PostMapping("/extract-majors")
    public ResponseEntity<List<String>> extractMajors(
            @RequestParam("file") MultipartFile file) throws IOException {

        List<String> majors = transcriptService.extractMajors(file);
        return ResponseEntity.ok(majors);
    }


    // 클라이언트에게 S3 업로드 URL 발급
    @PostMapping("/upload-url")
    public UploadUrlResponse generateUploadUrl(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UploadUrlRequest request
            ) {

        Long userId = userPrincipal.getUserId();

        return transcriptService.generateUploadUrl(userId, request.filename(), request.contentType());
    }


    // S3 업로드 완료 알림을 받고, 파싱 작업 처리
    @PostMapping("/parse")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void processUploadedFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody NotifyUploadCompleteRequest request
    ) {

        Long userId = userPrincipal.getUserId();

        transcriptService.processParsingJob(
                userId, request.fileKey(), request.major1(), request.major2(), request.major3()
        );
    }


}

