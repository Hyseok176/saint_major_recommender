package com.saintplus.course.controller;

import com.saintplus.common.security.JwtTokenProvider;
import com.saintplus.course.dto.RecommendedCourseDto;
import com.saintplus.course.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = {"http://localhost:3000", "https://saintplanner.cloud"}, allowCredentials = "true")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * AI 기반 추천 (프롬프트 입력)
     */
    @GetMapping("/ai")
    public ResponseEntity<List<RecommendedCourseDto>> getAiRecommendations(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String prompt,
            @RequestParam(required = false) String major
    ) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getUserId(token);
        
        // major가 없으면 사용자의 첫 번째 전공 사용 (필요시 구현)
        if (major == null || major.isEmpty()) {
            major = "CSE"; // 기본값
        }
        
        List<RecommendedCourseDto> recommendations = recommendationService.getAIRecommendations(userId, prompt, major);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 통계 기반 추천
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<RecommendedCourseDto>> getStatisticsRecommendations(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getUserId(token);
        
        List<RecommendedCourseDto> recommendations = recommendationService.getStatisticBasedRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }
}
