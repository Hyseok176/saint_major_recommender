package com.example.course_analyzer.controller;

import com.example.course_analyzer.domain.User;
import com.example.course_analyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController
 *
 * 사용자 인증(로그인, 회원가입)과 관련된 요청을 처리하는 컨트롤러입니다.
 *
 * 주요 기능:
 * 1. 메인 페이지(로그인 페이지) 렌더링
 * 2. 회원가입 처리
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 메인 페이지(인덱스)를 반환합니다.
     * 이미 로그인된 사용자는 결과 페이지(/results)로 리다이렉트합니다.
     *
     * URL: /
     *
     * @param authentication 현재 인증된 사용자 정보 (Spring Security)
     * @return index.html 템플릿 이름 또는 리다이렉트 URL
     */
    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            // 이미 로그인된 사용자는 결과 페이지로 이동
            return "redirect:/results";
        }
        return "index";
    }

    /**
     * 회원가입 요청을 처리합니다.
     * 사용자가 입력한 정보를 바탕으로 새로운 User 엔티티를 생성하고 DB에 저장합니다.
     *
     * URL: /register (POST)
     *
     * @param username           사용자 ID
     * @param password           비밀번호 (BCrypt로 암호화되어 저장됨)
     * @param nickname           닉네임 (선택 사항)
     * @param email              이메일 (선택 사항)
     * @param redirectAttributes 리다이렉트 시 화면에 전달할 메시지(성공/실패)를 담는 객체
     * @return 메인 페이지로 리다이렉트
     */
    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "email", required = false) String email,
            RedirectAttributes redirectAttributes) {
        // 1. 중복 ID 확인
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "이미 존재하는 사용자 ID입니다.");
            return "redirect:/";
        }

        // 2. 새로운 사용자 객체 생성 (Builder 패턴 사용)
        User user = User.builder()
                .username(username)
                .nickname(nickname)
                .email(email)
                .password(passwordEncoder.encode(password)) // 비밀번호 암호화 필수
                .build();

        // 3. DB 저장
        userRepository.save(user);

        // 4. 성공 메시지 전달 및 리다이렉트
        redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/";
    }
}
