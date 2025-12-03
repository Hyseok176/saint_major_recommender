package com.example.course_analyzer.controller;

import com.example.course_analyzer.domain.User;
import com.example.course_analyzer.repository.UserRepository;
import com.example.course_analyzer.service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

/**
 * TranscriptController
 *
 * 성적표 파일 처리와 관련된 요청을 담당하는 컨트롤러입니다.
 *
 * 주요 기능:
 * 1. 파일 업로드 폼 화면 제공
 * 2. 성적표 파일 업로드 및 파싱 처리
 * 3. 파일에서 전공 정보 추출 (API)
 */
@Controller
@RequiredArgsConstructor
public class TranscriptController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    /**
     * 파일 업로드 폼 페이지를 반환합니다.
     *
     * URL: /upload-form
     *
     * @return upload-file.html 템플릿 이름
     */
    @GetMapping("/upload-form")
    public String showUploadForm() {
        return "upload-file";
    }

    /**
     * 성적표 파일 업로드 요청을 처리합니다.
     * 업로드된 파일을 분석하여 사용자의 수강 기록을 업데이트합니다.
     *
     * URL: /upload (POST)
     *
     * @param file               업로드된 성적표 파일 (PDF, Excel 등)
     * @param major1             1전공
     * @param major2             2전공
     * @param major3             3전공
     * @param model              뷰 모델
     * @param request            HTTP 요청 객체 (IP 주소 로깅용)
     * @param redirectAttributes 리다이렉트 시 에러 메시지 전달용
     * @return 결과 페이지(/results) 또는 업로드 폼(/upload-form)으로 리다이렉트
     */
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam("major1") String major1,
            @RequestParam("major2") String major2,
            @RequestParam("major3") String major3,
            Model model,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        String ipAddress = request.getRemoteAddr();

        try {
            // 서비스 계층에 파일 분석 및 사용자 업데이트 위임
            courseService.updateUserTranscript(user, file, major1, major2, major3, ipAddress);
            return "redirect:/results";
        } catch (IOException e) {
            // 파일 처리 중 에러 발생 시 에러 메시지와 함께 업로드 페이지로 복귀
            redirectAttributes.addFlashAttribute("error", "파일 처리 중 오류가 발생했습니다. 파일 형식을 확인해주세요.");
            return "redirect:/upload-form";
        }
    }

    /**
     * 업로드된 파일에서 전공 정보를 추출하여 반환합니다.
     * 이 API는 파일을 서버에 저장하지 않고 내용만 분석합니다.
     *
     * URL: /extract-majors (POST)
     *
     * @param file 업로드된 파일
     * @return 추출된 전공 리스트 (JSON)
     */
    @PostMapping("/extract-majors")
    @ResponseBody
    public ResponseEntity<List<String>> extractMajors(@RequestParam("file") MultipartFile file) {
        try {
            List<String> majors = courseService.extractMajorsFromFile(file.getInputStream());
            return ResponseEntity.ok(majors);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // --- Helper Method ---
    // TODO: 공통 유틸리티 클래스로 분리 고려
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
}
