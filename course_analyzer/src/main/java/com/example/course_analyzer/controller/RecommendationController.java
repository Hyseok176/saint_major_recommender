package com.example.course_analyzer.controller;

import com.example.course_analyzer.domain.User;
import com.example.course_analyzer.dto.RecommendationRequestDto;
import com.example.course_analyzer.dto.RecommendedCourseDto;
import com.example.course_analyzer.repository.UserRepository;
import com.example.course_analyzer.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RecommendationController
 *
 * 과목 추천 기능과 관련된 요청을 처리하는 컨트롤러입니다.
 *
 * 주요 기능:
 * 1. 과목 추천 페이지 렌더링
 * 2. 사용자 맞춤형 과목 추천 로직 실행
 */
@Controller
@RequiredArgsConstructor
public class RecommendationController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    /**
     * 과목 추천 페이지를 반환합니다.
     * 초기 로딩 시 필요한 기본 데이터(미래 학기 옵션 등)를 모델에 담아 전달합니다.
     *
     * URL: /recommend
     *
     * @param model 뷰 모델
     * @return recommend.html 템플릿 이름
     */
    @GetMapping("/recommend")
    public String showRecommendPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        model.addAttribute("title", "과목 추천");
        // 초기에는 빈 추천 목록 전달
        model.addAttribute("recommendedCoursesMap", Map.of("major", new ArrayList<>(), "ge", new ArrayList<>()));
        // 미래 학기 선택 옵션 생성
        model.addAllAttributes(generateFutureSemesterOptions(user));
        return "recommend";
    }

    /**
     * 과목 추천 요청을 처리합니다. (AJAX 요청)
     * 사용자의 현재 상태, 장바구니, 제외 과목 등을 고려하여 최적의 과목을 추천합니다.
     * 결과는 HTML 조각(Fragment) 형태로 반환되어 페이지의 일부만 업데이트합니다.
     *
     * URL: /recommend (POST)
     *
     * @param requestDto 추천 요청 데이터 (장바구니 목록, 제외 목록, 대상 학기)
     * @param model 뷰 모델
     * @return recommend.html의 #recommendation-results 프래그먼트
     */
    @PostMapping("/recommend")
    public String recommend(@RequestBody RecommendationRequestDto requestDto, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        // Null Safety 처리
        List<String> cartCourseCodes = requestDto.getCartCourseCodes() == null ? new ArrayList<>() : requestDto.getCartCourseCodes();
        List<String> dismissedCourseCodes = requestDto.getDismissedCourseCodes() == null ? new ArrayList<>() : requestDto.getDismissedCourseCodes();
        Integer semester = requestDto.getSemester();

        // 서비스 계층에서 추천 로직 수행
        Map<String, List<RecommendedCourseDto>> recommendedCoursesMap = courseService.recommendCourses(user, cartCourseCodes, dismissedCourseCodes, semester);

        model.addAttribute("title", "과목 추천");
        model.addAttribute("recommendedCoursesMap", recommendedCoursesMap);
        // 필수 교양 트랙 미이수 여부 확인 (UI 표시용)
        model.addAttribute("isEssentialTrackRecommendation", courseService.hasUncompletedTracks(user));

        return "recommend :: #recommendation-results";
    }

    // --- Helper Methods ---

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String providerId = oauth2User.getName();
            return userRepository.findByProviderAndProviderId("kakao", providerId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with providerId: " + providerId));
        } else {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        }
    }

    /**
     * 사용자의 마지막 학기를 기준으로 향후 5년치 학기 옵션을 생성합니다.
     */
    private Map<String, Object> generateFutureSemesterOptions(User user) {
        int startYear;
        int startSemester;

        String lastSemester = user.getLastSemester(); // e.g., "2025-2"

        if (lastSemester != null && lastSemester.matches("\\d{4}-[12SW]")) {
            String[] parts = lastSemester.split("-");
            int lastYear = Integer.parseInt(parts[0]);
            String lastSem = parts[1];

            if (lastSem.equals("2") || lastSem.equals("W")) {
                startYear = lastYear + 1;
                startSemester = 1;
            } else { // "1" or "S"
                startYear = lastYear;
                startSemester = 2;
            }
        } else {
            LocalDate today = LocalDate.now();
            startYear = today.getYear();
            int currentMonth = today.getMonthValue();
            startSemester = (currentMonth >= 3 && currentMonth <= 8) ? 2 : 1;
            if (startSemester == 1 && currentMonth > 8) {
                startYear++;
            }
        }

        List<Integer> years = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            years.add(startYear + i);
        }

        return Map.of("futureYears", years, "startSemester", startSemester);
    }
}
