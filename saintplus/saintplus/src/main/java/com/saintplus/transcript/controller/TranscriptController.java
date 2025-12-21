package com.saintplus.transcript.controller;

import com.saintplus.common.security.UserPrincipal;
import com.saintplus.transcript.dto.NotifyUploadCompleteRequest;
import com.saintplus.transcript.dto.UploadUrlRequest;
import com.saintplus.transcript.dto.UploadUrlResponse;
import com.saintplus.transcript.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/transcripts")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://saintplanner.cloud"}, allowCredentials = "true")
public class TranscriptController {

    private final TranscriptService transcriptService;
    private final com.saintplus.common.security.JwtTokenProvider jwtTokenProvider;


    // 전공 먼저 추출
    @PostMapping("/extract-majors")
    public ResponseEntity<List<String>> extractMajors(
            @RequestParam("file") MultipartFile file) throws IOException {

        List<String> majors = transcriptService.extractMajors(file);
        return ResponseEntity.ok(majors);
    }

    // 파일 업로드 및 파싱 (직접 파싱 - S3 사용 안함)
    @PostMapping("/upload-and-parse")
    public ResponseEntity<Map<String, Object>> uploadAndParse(
            @RequestParam("file") MultipartFile file,
            @RequestParam("major1") String major1,
            @RequestParam(value = "major2", required = false, defaultValue = "") String major2,
            @RequestParam(value = "major3", required = false, defaultValue = "") String major3,
            @RequestHeader("Authorization") String token) throws IOException {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // JWT에서 userId 추출
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtTokenProvider.getUserId(jwtToken);
            
            // 파일 파싱 및 저장
            transcriptService.parseAndSaveTranscript(userId, file, major1, major2, major3);
            
            response.put("success", true);
            response.put("message", "성적표 파싱 및 저장이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "파싱 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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

