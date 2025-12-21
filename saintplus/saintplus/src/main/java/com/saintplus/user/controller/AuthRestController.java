package com.saintplus.user.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saintplus.user.service.UserService;
import com.saintplus.user.domain.User;
import com.saintplus.common.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AuthRestController
 *
 * React 프론트엔드를 위한 REST API 엔드포인트를 제공합니다.
 * JSON 형식의 요청/응답을 처리합니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://saintplanner.cloud"}, allowCredentials = "true")
public class AuthRestController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 REST API
     * 
     * @param request 회원가입 정보 (username, password, nickname, email)
     * @return 성공/실패 응답
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            userService.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getNickname(),
                request.getEmail()
            );
            
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "회원가입 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 로그인 REST API
     * 
     * @param request 로그인 정보 (username, password)
     * @return JWT 토큰과 사용자 정보
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 사용자 조회
            User user = userService.getUserByUsername(request.getUsername());
            
            // 비밀번호 확인
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                response.put("success", false);
                response.put("message", "아이디 또는 비밀번호가 잘못되었습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // JWT 토큰 생성
            String token = jwtTokenProvider.createToken(user.getId());
            
            response.put("success", true);
            response.put("token", token);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "email", user.getEmail() != null ? user.getEmail() : ""
            ));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "아이디 또는 비밀번호가 잘못되었습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * 사용자 전공 정보 업데이트 REST API
     * 
     * @param request 전공 정보 (major1, major2, major3)
     * @return 성공/실패 응답
     */
    @PostMapping("/update-majors")
    public ResponseEntity<Map<String, Object>> updateMajors(
            @RequestBody UpdateMajorsRequest request,
            @RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // JWT 토큰에서 userId 추출
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtTokenProvider.getUserId(jwtToken);
            
            System.out.println("===== 전공 정보 업데이트 =====");
            System.out.println("User ID: " + userId);
            System.out.println("Major1: " + request.getMajor1());
            System.out.println("Major2: " + request.getMajor2());
            System.out.println("Major3: " + request.getMajor3());
            
            // 전공 정보 업데이트
            userService.updateUserData(
                userId,
                request.getMajor1() != null ? request.getMajor1() : "",
                request.getMajor2() != null ? request.getMajor2() : "",
                request.getMajor3() != null ? request.getMajor3() : ""
            );
            
            System.out.println("전공 정보 저장 완료");
            
            response.put("success", true);
            response.put("message", "전공 정보가 저장되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("전공 정보 저장 오류: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "전공 정보 저장에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 회원가입 요청 DTO
     */
    public static class RegisterRequest {
        private String username;
        private String password;
        private String nickname;
        private String email;

        public RegisterRequest() {}

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * 로그인 요청 DTO
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {}

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * 전공 업데이트 요청 DTO
     */
    public static class UpdateMajorsRequest {
        private String major1;
        private String major2;
        private String major3;

        public UpdateMajorsRequest() {}

        public String getMajor1() {
            return major1;
        }

        public void setMajor1(String major1) {
            this.major1 = major1;
        }

        public String getMajor2() {
            return major2;
        }

        public void setMajor2(String major2) {
            this.major2 = major2;
        }

        public String getMajor3() {
            return major3;
        }

        public void setMajor3(String major3) {
            this.major3 = major3;
        }
    }
}
