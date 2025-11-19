package com.saintplus.controller;

import com.saintplus.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final RecommendationService recommendationService;

    // 브라우저 테스트용: http://localhost:8080/test-ai?userId=1234
    @GetMapping("/test-ai")
    public List<String> test(@RequestParam Long userId) {
        return recommendationService.getRecommendCourses(userId);
    }
}