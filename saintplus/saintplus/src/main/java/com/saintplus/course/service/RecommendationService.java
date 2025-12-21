package com.saintplus.course.service;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.saintplus.course.dto.AiRecommendRequest;
import com.saintplus.course.dto.AiRecommendResponse;
import com.saintplus.course.dto.AiRecommendationDto;
import com.saintplus.course.dto.RecommendedCourseDto;
import com.saintplus.course.repository.CourseRepository;
import com.saintplus.course.repository.CourseMappingRepository;
import com.saintplus.transcript.domain.Enrollment;
import com.saintplus.transcript.repository.EnrollmentRepository;
import com.saintplus.user.domain.User;
import com.saintplus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.saintplus.course.domain.Course;
import com.saintplus.course.domain.CourseMapping;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {


    private final WebClient aiWebClient;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseMappingRepository courseMappingRepository;
    private final CourseService courseService;
    private final UserService userService;

    // 데이터가 이 숫자보다 적으면 통계, 많으면 AI 사용
    private static final int DATA_THRESHOLD = 1000;


    // [AI 추천] Python 서버에 요청
    public List<RecommendedCourseDto> getAIRecommendations(Long userId, String prompt, String major) {
        System.out.println("=== AI 추천 시작 ===");
        System.out.println("userId: " + userId + ", prompt: " + prompt + ", major: " + major);
        
        try {
            // 1. DB에서 사용자가 이미 수강한 과목 코드 조회
            Set<String> takenCourseCodes = enrollmentRepository.findAllByUserId(userId).stream()
                    .map(Enrollment::getCourseCode)
                    .collect(Collectors.toSet());
            System.out.println("수강한 과목 개수: " + takenCourseCodes.size());

            // 2. 파이썬 서버에 POST 요청 (Request DTO 전달)
            AiRecommendRequest requestBody = new AiRecommendRequest(prompt, major, 0.1);
            System.out.println("Python 서버 요청 전송: " + requestBody);

            AiRecommendResponse response = aiWebClient.post()
                    .uri("/recommend")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(AiRecommendResponse.class)
                    .block();
            
            System.out.println("Python 서버 응답 받음: " + (response != null ? response.getResults().size() + "개 결과" : "null"));

            if (response == null || response.getResults() == null) {
                System.out.println("응답이 null이거나 결과가 없음");
                return new ArrayList<>();
            }

            // 3. course_mapping 테이블에 실제로 존재하는 과목만 필터링 (이번 학기 개설 과목)
            List<String> allRecommendedCodes = response.getResults().stream()
                    .map(AiRecommendResponse.AiCourseItem::getCode)
                    .toList();
            List<CourseMapping> availableCourseMappings = courseMappingRepository.findAllById(allRecommendedCodes);
            Set<String> availableCourseCodes = availableCourseMappings.stream()
                    .map(CourseMapping::getCourseCode)
                    .collect(Collectors.toSet());
            System.out.println("Python 추천 중 이번 학기 개설 과목: " + availableCourseCodes.size() + "개");

            // 4. 개설되고 + 수강하지 않은 과목 중 상위 5개 추출
            List<AiRecommendResponse.AiCourseItem> topItems = response.getResults().stream()
                    .filter(item -> availableCourseCodes.contains(item.getCode()))  // course_mapping에 있는 과목만
                    .filter(item -> !takenCourseCodes.contains(item.getCode()))    // 수강 안 한 과목만
                    .sorted(Comparator.comparingDouble(AiRecommendResponse.AiCourseItem::getScore).reversed())
                    .limit(5)
                    .toList();
            System.out.println("최종 추천 과목: " + topItems.size() + "개");

            // 5. CourseMapping을 Course로 변환하여 반환
            Map<String, CourseMapping> courseMappingMap = availableCourseMappings.stream()
                    .collect(Collectors.toMap(CourseMapping::getCourseCode, cm -> cm));

            List<RecommendedCourseDto> result = topItems.stream()
                    .map(item -> {
                        CourseMapping mapping = courseMappingMap.get(item.getCode());
                        Course course = new Course(mapping.getCourseCode(), mapping.getCourseName());
                        course.setSemester(mapping.getSemester());
                        return RecommendedCourseDto.builder()
                                .score(item.getScore())
                                .course(course)
                                .build();
                    })
                    .toList();
            
            System.out.println("최종 반환 결과: " + result.size() + "개");
            return result;

        } catch (Exception e) {
            System.err.println("=== AI 추천 실패 ===");
            System.err.println("오류 타입: " + e.getClass().getName());
            System.err.println("오류 메시지: " + e.getMessage());
            e.printStackTrace();
            // 실패 시 빈 리스트 혹은 기본 추천 반환
            return new ArrayList<>();
        }
    }


    // [통계 추천]
    public List<RecommendedCourseDto> getStatisticBasedRecommendations(Long userId) {
        User user = userService.getUserById(userId);

        Map<String, List<RecommendedCourseDto>> statResult =
                courseService.recommendCourses(
                        user,
                        List.of(),   // cartCourseCodes (지금은 비워두거나 추후 전달)
                        List.of(),   // dismissedCourseCodes
                        null         // semester
                );

        return statResult.values().stream()
                .flatMap(List::stream)
                .distinct()
                .limit(10)
                .toList();
    }
}