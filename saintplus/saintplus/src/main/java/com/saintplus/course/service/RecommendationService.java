package com.saintplus.course.service;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.saintplus.course.dto.AiRecommendRequest;
import com.saintplus.course.dto.AiRecommendResponse;
import com.saintplus.course.dto.AiRecommendationDto;
import com.saintplus.course.dto.RecommendedCourseDto;
import com.saintplus.course.repository.CourseRepository;
import com.saintplus.transcript.domain.Enrollment;
import com.saintplus.transcript.repository.EnrollmentRepository;
import com.saintplus.user.domain.User;
import com.saintplus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.saintplus.course.domain.Course;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {


    private final WebClient aiWebClient;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final UserService userService;

    // 데이터가 이 숫자보다 적으면 통계, 많으면 AI 사용
    private static final int DATA_THRESHOLD = 1000;


    // [AI 추천] Python 서버에 요청
    public List<RecommendedCourseDto> getAIRecommendations(Long userId, String prompt, String major) {
        try {
            // 1. DB에서 사용자가 이미 수강한 과목 코드 조회
            Set<String> takenCourseCodes = enrollmentRepository.findAllByUserId(userId).stream()
                    .map(Enrollment::getCourseCode)
                    .collect(Collectors.toSet());

            // 2. 파이썬 서버에 POST 요청 (Request DTO 전달)
            AiRecommendRequest requestBody = new AiRecommendRequest(prompt, major, 0.1);

            AiRecommendResponse response = aiWebClient.post()
                    .uri("/recommend")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(AiRecommendResponse.class)
                    .block();

            if (response == null || response.getResults() == null) return new ArrayList<>();

            // 3. 필터링 및 상위 5개 추출
            List<AiRecommendResponse.AiCourseItem> topItems = response.getResults().stream()
                    .filter(item -> !takenCourseCodes.contains(item.getCode()))
                    .sorted(Comparator.comparingDouble(AiRecommendResponse.AiCourseItem::getScore).reversed())
                    .limit(5)
                    .toList();

            // 2. 5개 과목 정보를 DB에서 한 번에 조회 (성능 최적화)
            List<String> topCodes = topItems.stream().map(AiRecommendResponse.AiCourseItem::getCode).toList();
            List<Course> dbCourses = courseRepository.findAllById(topCodes);

            // 3. 안전하게 매핑
            return topItems.stream().map(item -> {
                Course courseInfo = dbCourses.stream()
                        .filter(c -> c.getCourseCode().equals(item.getCode()))
                        .findFirst()
                        .orElseGet(() -> new Course(item.getCode(), "정보 없음")); // 데이터가 없어도 에러 안 남

                return RecommendedCourseDto.builder()
                        .score(item.getScore())
                        .course(courseInfo)
                        .build();
            }).toList();

        } catch (Exception e) {
            System.err.println("AI 서버 통신 실패: " + e.getMessage());
            // 실패 시 빈 리스트 혹은 기본 추천 반환
            return new ArrayList<>();
        }
    }


    // [통계 추천] (데이터 부족하거나 AI 서버 죽었을 때)
    public List<String> getStatisticBasedRecommendations(Long userId) {
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
                .map(dto -> dto.getCourse().getCourseCode()) // or getCourseName()
                .distinct()
                .limit(10)
                .toList();
    }
}