package com.saintplus.transcript.service;

import com.saintplus.course.domain.Course;
import com.saintplus.transcript.domain.Enrollment;
import com.saintplus.course.dto.CourseAnalysisData;
import com.saintplus.transcript.dto.TranscriptParsingResult;
import com.saintplus.transcript.dto.TranscriptScanResult;
import com.saintplus.course.repository.CourseRepository;
import com.saintplus.transcript.repository.EnrollmentRepository;
import com.saintplus.user.domain.User;
import com.saintplus.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptParsingWorker {

    private final S3Client s3Client;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final TranscriptParser transcriptParser;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;


    public void processParingAndSaving(Long userId, String fileKey) {

        try {
            log.info("Start synchronous parsing job. fileKey={}, userId={}", fileKey, userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            byte[] fileBytes = loadFileFromS3(fileKey);


            // 파일 내용 분석, 전처리 및 분류, 새로운 과목 및 수강이력 DB 저장
            TranscriptScanResult rawResult = transcriptParser.analyzeFile(new ByteArrayInputStream(fileBytes), userId.toString());

            saveNewCoursesToDatabase(rawResult.getMappingCourseCodeName(), userId.toString());

            TranscriptParsingResult parsed = transcriptParser.groupAndFormatCourses(rawResult.getRawCourses());

            saveEnrollmentToDatabase(user, parsed.getCoursesBySemester());


            // S3에서 삭제
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFileFromS3(fileKey);
                    log.info("Parsing complete & file deleted. fileKey={}", fileKey);
                }
            });

        } catch (Exception e) {
            log.error("Failed to process parsing request. Transaction will be rolled back, and SQS message will be retried.", e);
            throw new RuntimeException("SQS message processing failed.", e);
        }

    }



    @Transactional
    private void saveNewCoursesToDatabase(Map<String,String> mappingCourseCodeName, String userId){

        Set<String> courseCodesFromTranscript = mappingCourseCodeName.keySet();

        // 기존 과목 조회
        List<Course> existingCourses = courseRepository.findAllById(courseCodesFromTranscript);
        Set<String> existingCourseCodes = existingCourses.stream()
                .map(Course::getCourseCode)
                .collect(Collectors.toSet());

        // DB에 없는 새로운 과목만 필터링, Course 엔티티로 변환
        List<Course> newCourses = courseCodesFromTranscript.stream()
                .filter(code -> !existingCourseCodes.contains(code))
                .map(code -> {
                    Course course = new Course();
                    course.setCourseCode(code);
                    course.setCourseName(mappingCourseCodeName.get(code));
                    course.setSemester(4); // 임의의 값 유지
                    return course;
                })
                .toList();

        // 새로운 과목 DB 저장
        if (!newCourses.isEmpty()) {
            courseRepository.saveAll(newCourses);
            for (Course course : newCourses) {
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                log.info("NewCourse: {}, User: {}, CODE: {}, SEMESTER: 4", timestamp, userId, course.getCourseCode());
            }
        }
    }



    public void saveEnrollmentToDatabase(User user, Map<String, List<CourseAnalysisData>> coursesBySemester) {

        enrollmentRepository.deleteByUser(user);

        coursesBySemester.forEach((semesterString, courses) -> {
            double semesterNumber;
            Pattern pattern = Pattern.compile("^^([\\d\\.]+)\\학기");
            Matcher matcher = pattern.matcher(semesterString);
            if (matcher.find()) {
                semesterNumber = Double.parseDouble(matcher.group(1));
            } else {
                semesterNumber = 0.0;
            }

            courses.forEach(course -> {
                Enrollment enrollment = Enrollment.builder()
                        .user(user)
                        .courseCode(course.getCourseCode())
                        .importantRemarks(course.getImportantRemarks())
                        .semester(semesterNumber)
                        .build();
                enrollmentRepository.save(enrollment);
            });
        });
    }



    private byte[] loadFileFromS3(String fileKey){

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);

            return objectBytes.asByteArray();

        } catch (NoSuchKeyException e) {
            log.error("S3 file not found. key={}", fileKey);
            throw new IllegalStateException("Uploaded file not found in storage.", e);

        } catch (Exception e) {
            log.error("Failed to load S3 file. key={}", fileKey, e);
            throw new RuntimeException("Failed to load file from S3.", e);
        }

    }



    private void deleteFileFromS3(String fileKey) {

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteRequest);

            log.info("S3 file deleted. key={}", fileKey);

        } catch (Exception e) {
            log.error("Failed to delete S3 file. key={}", fileKey, e);
        }

    }

}
