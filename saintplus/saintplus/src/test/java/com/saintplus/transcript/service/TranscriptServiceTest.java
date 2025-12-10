package com.saintplus.transcript.service;

import com.saintplus.transcript.dto.UploadUrlResponse;
import com.saintplus.transcript.util.StorageClient;
import com.saintplus.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class TranscriptServiceTest {

    @Mock
    StorageClient storageClient;

    @Mock
    TranscriptParsingWorker transcriptParsingWorker;

    @Mock
    TranscriptParser transcriptParser;

    @Mock
    UserService userService;

    @InjectMocks
    TranscriptService transcriptService;


    @Test
    void extractMajors_success() throws Exception {
        when(transcriptParser.extractMajorsFromFile(any())).thenReturn(List.of("컴공", "수학"));

        var file = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "dummy".getBytes()
        );

        List<String> result = transcriptService.extractMajors(file);
        assertEquals(2, result.size());
    }


    @Test
    void generateUploadUrl_success() {
        UploadUrlResponse mockRes = new UploadUrlResponse("url", "key");
        when(storageClient.generatePresignedUrl(anyString(), anyString())).thenReturn(mockRes);

        UploadUrlResponse res = transcriptService.generateUploadUrl(1L, "abc.txt", "text/plain");

        assertEquals("url", res.uploadUrl());
    }


    @Test
    void processParsingJob_success() {

        doNothing().when(userService).updateUserData(anyLong(), anyString(), anyString(), anyString());
        doNothing().when(transcriptParsingWorker).processParingAndSaving(anyLong(), anyString());

        transcriptService.processParsingJob(1L, "fileKey", "a", "b", "c");

        verify(userService).updateUserData(1L, "a", "b", "c");
        verify(transcriptParsingWorker).processParingAndSaving(1L, "fileKey");
    }

}

