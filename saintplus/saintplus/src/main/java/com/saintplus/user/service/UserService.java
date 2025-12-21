package com.saintplus.user.service;

import com.saintplus.user.domain.User;
import com.saintplus.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void updateUserData(Long userId, String major1, String major2, String major3) {

        System.out.println("===== updateUserData 시작 =====");
        System.out.println("입력 - userId: " + userId + ", major1: " + major1 + ", major2: " + major2 + ", major3: " + major3);
        
        //사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        System.out.println("변경 전 - major1: " + user.getMajor1() + ", major2: " + user.getMajor2() + ", major3: " + user.getMajor3());
        
        //사용자 정보 업데이트
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(java.time.LocalDateTime.now());
        }

        user.setMajor1(major1.replace(" ", ""));
        user.setMajor2(major2.replace(" ", ""));
        user.setMajor3(major3.replace(" ", ""));
        
        System.out.println("변경 후 - major1: " + user.getMajor1() + ", major2: " + user.getMajor2() + ", major3: " + user.getMajor3());
        
        User savedUser = userRepository.save(user);
        
        System.out.println("저장 완료 - major1: " + savedUser.getMajor1() + ", major2: " + savedUser.getMajor2() + ", major3: " + savedUser.getMajor3());
        System.out.println("===== updateUserData 종료 =====");
    }

    /**
     * 회원가입 처리
     */
    @Transactional
    public void registerUser(String username, String password, String nickname, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("이미 존재하는 사용자 ID입니다.");
        }

        User user = User.builder()
                .username(username)
                .nickname(nickname)
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();

        userRepository.save(user);
    }

    /**
     * Authentication 객체로부터 User 엔티티 조회
     */
    public User getUserFromAuthentication(Authentication authentication) {
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
     * username으로 User 조회
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
    }
}
