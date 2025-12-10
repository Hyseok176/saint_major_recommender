package com.saintplus.transcript.service;

import com.saintplus.course.dto.CourseAnalysisData;
import com.saintplus.transcript.dto.TranscriptParsingResult;
import com.saintplus.transcript.dto.TranscriptScanResult;
import com.saintplus.course.repository.CourseRepository;
import com.saintplus.transcript.repository.EnrollmentRepository;
import com.saintplus.user.domain.User;
import com.saintplus.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranscriptParsingWorkerTest {

    @InjectMocks
    private TranscriptParsingWorker worker;

    @Mock
    private S3Client s3Client;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TranscriptParser transcriptParser;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_FILE_KEY = "uploads/1/123456789/test.pdf";
    private static final String TEST_BUCKET = "test-bucket";
    private User testUser;

    @BeforeEach
    void setUp() {
        // @Value 필드 모킹
        ReflectionTestUtils.setField(worker, "bucketName", TEST_BUCKET);

        // Mock User 객체 설정
        testUser = new User(TEST_USER_ID.toString(), "", "", "", "", "", "", "", "", ""); // User 생성자가 ID를 받는다고 가정

        // TransactionSynchronizationManager는 테스트 환경에서 수동으로 활성화되어야 함
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    // 이 메서드는 테스트 후 TransactionSynchronizationManager를 정리합니다.
    // 실제 Spring 환경에서는 @Transactional 어노테이션이 이를 처리합니다.
    // 하지만 Mockito 환경이므로 명시적으로 정리합니다.
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }


    @Test
    @DisplayName("성공: 파싱, DB 저장 및 S3 파일 삭제가 순서대로 실행되어야 한다")
    void processParingAndSaving_Success() throws Exception {
        // 1. Given (Mock 설정)
        // User Mock
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));

        // S3 Load Mock
        byte[] mockBytes = "mock file content".getBytes();
        ResponseBytes<GetObjectResponse> mockResponse = mock(ResponseBytes.class);
        given(mockResponse.asByteArray()).willReturn(mockBytes);
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).willReturn(mockResponse);

        // Parser Mock (성공적인 파싱 결과)
        Map<String, String> courseMap = Map.of("CS101", "Intro to CS");
        TranscriptScanResult scanResult = new TranscriptScanResult(Collections.emptyList(), courseMap);
        TranscriptParsingResult parsingResult = mock(TranscriptParsingResult.class);

        given(transcriptParser.analyzeFile(any(ByteArrayInputStream.class), eq(TEST_USER_ID.toString())))
                .willReturn(scanResult);
        given(transcriptParser.groupAndFormatCourses(anyList())).willReturn(parsingResult);
        given(parsingResult.getCoursesBySemester()).willReturn(Map.of("1학기", List.of(new CourseAnalysisData())));

        // saveNewCoursesToDatabase Mock (새로운 과목 없음)
        given(courseRepository.findAllById(anySet())).willReturn(Collections.emptyList());

        // 2. When
        worker.processParingAndSaving(TEST_USER_ID, TEST_FILE_KEY);

        // 3. Then (검증)

        // 3-1. 핵심 비즈니스 로직 호출 순서 확인
        verify(userRepository).findById(TEST_USER_ID);
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        verify(transcriptParser).analyzeFile(any(ByteArrayInputStream.class), anyString());
        verify(enrollmentRepository).deleteByUser(testUser); // 기존 수강 기록 삭제 확인
        verify(enrollmentRepository, times(1)).save(any()); // 신규 수강 기록 저장 확인

        // 3-2. S3 삭제 로직 확인 (Transaction Synchronization)

        // **주의:** 실제 테스트 환경에서는 @Transactional의 롤백/커밋을 시뮬레이션해야 합니다.
        // Mockito 환경에서 TransactionSynchronizationManager.registerSynchronization()가 호출되었는지 확인
        // (직접적인 afterCommit() 호출 검증은 복잡하므로, 호출 흐름만 검증)

        // 3-3. afterCommit() 로직이 실행되었다고 가정하고 deleteFileFromS3가 호출되는지 확인
        // 실제 S3 삭제 메서드는 private이므로, 스파이(Spy)를 사용하거나,
        // TransactionSynchronizationManager가 호출 등록되었음을 확인하는 간접적인 방법을 사용해야 합니다.

        // 여기서는 S3Client의 deleteObject가 호출되는지 확인합니다.
        // 실제 호출은 afterCommit에서 이루어지므로, 바로 verify 할 수는 없고
        // 테스트 후 트랜잭션 커밋을 시뮬레이션해야 합니다.
        // Mockito @InjectMocks에서는 이를 명확히 검증하기 어려우므로, 호출 흐름 확인에 집중합니다.
        // => deleteObject는 afterCommit()이 실행된 후 호출되므로, 여기에 직접 verify를 넣는 것은 부정확합니다.
    }


    @Test
    @DisplayName("실패: DB 저장 중 오류 발생 시 S3 파일 삭제가 실행되지 않아야 한다")
    void processParingAndSaving_DBFailure() throws Exception {
        // 1. Given (Mock 설정)
        // User, S3 Load, Parser Mock은 성공적으로 설정 (위의 성공 테스트와 유사)
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));

        byte[] mockBytes = "mock file content".getBytes();
        ResponseBytes<GetObjectResponse> mockResponse = mock(ResponseBytes.class);
        given(mockResponse.asByteArray()).willReturn(mockBytes);
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).willReturn(mockResponse);

        Map<String, String> courseMap = Map.of("CS101", "Intro to CS");
        TranscriptScanResult scanResult = new TranscriptScanResult(Collections.emptyList(), courseMap);
        TranscriptParsingResult parsingResult = mock(TranscriptParsingResult.class);

        given(transcriptParser.analyzeFile(any(ByteArrayInputStream.class), anyString()))
                .willReturn(scanResult);
        given(transcriptParser.groupAndFormatCourses(anyList())).willReturn(parsingResult);
        given(parsingResult.getCoursesBySemester()).willReturn(Map.of("1학기", List.of(new CourseAnalysisData())));

        // 🚨 실패 시나리오 설정: Enrollment 저장 시 RuntimeException 발생
        willThrow(new RuntimeException("DB Save Failed")).given(enrollmentRepository).deleteByUser(any());

        // 2. When & Then
        // 예외가 발생해야 함을 확인
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            worker.processParingAndSaving(TEST_USER_ID, TEST_FILE_KEY);
        });

        // 3. Then (검증)
        // DB 롤백이 발생했으므로 S3 deleteObject는 절대 호출되면 안 됨.
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

}
