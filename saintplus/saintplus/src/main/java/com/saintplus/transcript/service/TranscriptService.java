package com.saintplus.transcript.service;

import com.saintplus.transcript.dto.UploadUrlResponse;
import com.saintplus.transcript.util.StorageClient;
import com.saintplus.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final StorageClient storageClient;
    private final TranscriptParsingWorker transcriptParsingWorker;
    private final TranscriptParser transcriptParser;
    private final UserService userService;


    public List<String> extractMajors(MultipartFile file) throws IOException {
        return transcriptParser.extractMajorsFromFile(file.getInputStream());
    }

    @Transactional
    public void parseAndSaveTranscript(Long userId, MultipartFile file, String major1, String major2, String major3) throws IOException {
        log.info("Starting direct parsing for userId: {}", userId);
        
        // 전공 정보 업데이트
        userService.updateUserData(userId, major1, major2, major3);
        
        // 파일을 임시로 저장하고 파싱
        String tempFileKey = "temp/" + userId + "/" + System.currentTimeMillis() + "/" + file.getOriginalFilename();
        
        // Worker에게 파싱 작업 위임 (파일 내용을 직접 전달)
        transcriptParsingWorker.parseFromInputStream(userId, file.getInputStream());
        
        log.info("Direct parsing completed for userId: {}", userId);
    }



    public UploadUrlResponse generateUploadUrl(Long userId, String filename, String contentType) {

        //S3 Key 생성
        String fileKey = String.format("uploads/%d/%d/%s",
                userId,
                System.currentTimeMillis(),
                filename);

        //StorageClient에 Presigned URL 생성 위임.
        return storageClient.generatePresignedUrl(fileKey, contentType);

    }



    public void processParsingJob(Long userId, String fileKey, String major1, String major2, String major3) {
        log.info("Starting synchronous parsing job for userId: {}, fileKey: {}", userId, fileKey);

        // 사용자 전공 정보 업데이트
        userService.updateUserData(userId, major1, major2, major3);

        // Worker 호출하여 파싱 및 저장 작업 위임
        transcriptParsingWorker.processParingAndSaving(userId, fileKey);

        log.info("Parsing job completed for userId: {}", userId);
    }


}
