package com.saintplus.coursehistory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saintplus.common.security.JwtTokenProvider;
import com.saintplus.common.security.UserPrincipal;
import com.saintplus.coursehistory.dto.NotifyUploadCompleteRequest;
import com.saintplus.coursehistory.dto.UploadUrlRequest;
import com.saintplus.coursehistory.dto.UploadUrlResponse;
import com.saintplus.coursehistory.service.TranscriptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(TranscriptController.class)
@WithMockUser(username = "test@example.com", roles = {"USER"})
class TranscriptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TranscriptService transcriptService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private UserPrincipal mockUser;

    @BeforeEach
    void setUp() {
        // Mock UserPrincipal 설정
        mockUser = new UserPrincipal(100L);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("POST /extract-majors: 전공 추출 테스트")
    @WithMockUser(roles = "USER")
    void extractMajors_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transcript.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Transcript content including major info".getBytes(StandardCharsets.UTF_8)
        );
        List<String> expectedMajors = Arrays.asList("컴퓨터공학", "경영학", "");
        given(transcriptService.extractMajors(any(MultipartFile.class))).willReturn(expectedMajors);

        // When & Then
        mockMvc.perform(multipart("/api/v1/transcripts/extract-majors")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("컴퓨터공학"))
                .andExpect(jsonPath("$[1]").value("경영학"));

        verify(transcriptService).extractMajors(any(MultipartFile.class));
    }

    @Test
    @DisplayName("POST /upload-url: S3 Presigned URL 생성 테스트")
    @WithMockUser(roles = "USER")
    void generateUploadUrl_Success() throws Exception {
        // Given
        UploadUrlRequest request = new UploadUrlRequest("transcript.pdf", "application/pdf");
        UploadUrlResponse expectedResponse = new UploadUrlResponse("https://presigned.url/testkey", "testkey");

        given(transcriptService.generateUploadUrl(anyLong(), anyString(), anyString()))
                .willReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transcripts/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value(expectedResponse.uploadUrl()))
                .andExpect(jsonPath("$.fileKey").value(expectedResponse.fileKey()));

        verify(transcriptService).generateUploadUrl(eq(mockUser.getUserId()), eq(request.filename()), eq(request.contentType()));
    }

    @Test
    @DisplayName("POST /parse: S3 업로드 완료 후 파싱 작업 처리 테스트 (동기)")
    @WithMockUser(roles = "USER")
    void processUploadedFile_Success() throws Exception {
        // Given
        NotifyUploadCompleteRequest request = new NotifyUploadCompleteRequest(
                "uploads/100/123456789/transcript.txt", "컴퓨터공학", "경영학", ""
        );
        // processParsingJob은 void 메서드이므로, 호출만 확인하면 됨.

        // When & Then
        mockMvc.perform(post("/api/v1/transcripts/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                // 이전 SQS 기반에서는 ACCEPTED (202)였으나, 동기 처리이므로 200 또는 204(No Content)가 적합.
                // 현재 컨트롤러 코드가 HttpStatus.ACCEPTED(202)를 사용하므로, 202를 기대해야 함.
                .andExpect(status().isAccepted()); // @ResponseStatus(HttpStatus.ACCEPTED)에 따름

        verify(transcriptService).processParsingJob(
                eq(mockUser.getUserId()),
                eq(request.fileKey()),
                eq(request.major1()),
                eq(request.major2()),
                eq(request.major3())
        );
    }
}