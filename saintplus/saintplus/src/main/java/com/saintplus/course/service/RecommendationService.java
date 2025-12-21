package com.saintplus.course.service;

import com.saintplus.course.dto.AiRecommendationDto;
import com.saintplus.course.dto.RecommendedCourseDto;
import com.saintplus.transcript.repository.EnrollmentRepository;
import com.saintplus.user.domain.User;
import com.saintplus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendationService {


    private final WebClient aiWebClient;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final UserService userService;

    // 데이터가 이 숫자보다 적으면 통계, 많으면 AI 사용
    private static final int DATA_THRESHOLD = 1000;


    public List<String> getRecommendCourses(Long userId) {
        // 실제 DB에서 유저 수강 이력 개수를 조회(count)해서 넣으세요.
        long currentDataCount = enrollmentRepository.countByUserId(userId);
        //long currentDataCount = 5000; // 지금은 테스트니까 5000개 있다고 가정

        if (currentDataCount < DATA_THRESHOLD) {
            return getStatisticBasedRecommendations(userId);
        } else {
            return getMlBasedRecommendations(userId);
        }
    }


    // [AI 추천] Python 서버에 요청
    private List<String> getMlBasedRecommendations(Long userId) {
        try {
            AiRecommendationDto response = aiWebClient.get()
                    .uri("/recommend/" + userId) // Python API 주소
                    .retrieve()
                    .bodyToMono(AiRecommendationDto.class)
                    .block(); // 응답 기다림

            return response != null ? response.getRecommendations() : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("AI 서버 통신 실패: " + e.getMessage());
            return getStatisticBasedRecommendations(userId); // 실패하면 통계 추천으로 대체
        }
    }


    // [통계 추천] (데이터 부족하거나 AI 서버 죽었을 때)
    private List<String> getStatisticBasedRecommendations(Long userId) {
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