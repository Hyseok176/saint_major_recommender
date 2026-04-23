package com.saintplus.course.controller;

import com.saintplus.common.security.JwtTokenProvider;
import com.saintplus.course.dto.RecommendedCourseDto;
import com.saintplus.course.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 통합 추천 엔드포인트
     * type=statistics | ai
     */
    @GetMapping
    public ResponseEntity<List<RecommendedCourseDto>> getRecommendations(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "statistics") String type,
            @RequestParam(required = false) String prompt,
            @RequestParam(required = false) String major
    ) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getUserId(token);

        String normalizedType = type.toLowerCase(Locale.ROOT);
        if ("ai".equals(normalizedType)) {
            if (prompt == null || prompt.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            if (major == null || major.isBlank()) {
                major = "CSE";
            }
            return ResponseEntity.ok(recommendationService.getAIRecommendations(userId, prompt, major));
        }

        if ("statistics".equals(normalizedType)) {
            return ResponseEntity.ok(recommendationService.getStatisticBasedRecommendations(userId));
        }

        return ResponseEntity.badRequest().build();
    }

    /**
     * AI 기반 추천 (프롬프트 입력)
     */
    @GetMapping("/ai")
    public ResponseEntity<List<RecommendedCourseDto>> getAiRecommendations(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String prompt,
            @RequestParam(required = false) String major
    ) {
        return getRecommendations(authHeader, "ai", prompt, major);
    }

    /**
     * 통계 기반 추천
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<RecommendedCourseDto>> getStatisticsRecommendations(
            @RequestHeader("Authorization") String authHeader
    ) {
        return getRecommendations(authHeader, "statistics", null, null);
    }
}
