package com.saintplus.recommendation.service;

import com.saintplus.recommendation.dto.AiRecommendationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final WebClient aiWebClient;

    // 데이터가 이 숫자보다 적으면 통계, 많으면 AI 사용
    private static final int DATA_THRESHOLD = 1000;

    public List<String> getRecommendCourses(Long userId) {
        // TODO: 나중에 실제 DB에서 유저 수강 이력 개수를 조회(count)해서 넣으세요.
        long currentDataCount = 5000; // 지금은 테스트니까 5000개 있다고 가정

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
        // TODO: 나중에 실제 SQL 쿼리 로직으로 바꾸세요.
        return List.of("기초프로그래밍", "선형대수학", "컴퓨터개론 (통계기반)");
    }
}