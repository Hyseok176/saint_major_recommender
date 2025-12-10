package com.saintplus.user.service;

import com.saintplus.user.domain.User;
import com.saintplus.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserService 테스트
 * 
 * 사용자 관련 비즈니스 로직의 정상 작동을 확인합니다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 - 성공")
    void testRegisterUser_Success() {
        // Given
        String username = "newuser";
        String password = "password123";
        String nickname = "닉네임";
        String email = "test@example.com";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // When
        userService.registerUser(username, password, nickname, email);

        // Then
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).encode(password);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 - 중복 사용자 실패")
    void testRegisterUser_DuplicateUsername() {
        // Given
        String username = "existinguser";
        User existingUser = User.builder()
                .username(username)
                .password("password")
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(username, "password", "nickname", "email"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 존재하는 사용자 ID입니다.");

        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 데이터 업데이트 - 성공")
    void testUpdateUserData_Success() {
        // Given
        Long userId = 1L;
        String major1 = "컴퓨터공학";
        String major2 = "수학";
        String major3 = "";

        User mockUser = User.builder()
                .username("testuser")
                .password("password")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // When
        userService.updateUserData(userId, major1, major2, major3);

        // Then
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
        assertThat(mockUser.getMajor1()).isEqualTo("컴퓨터공학");
        assertThat(mockUser.getMajor2()).isEqualTo("수학");
    }

    @Test
    @DisplayName("사용자 조회 - 존재하지 않는 사용자")
    void testGetUserByUsername_NotFound() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername(username))
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findByUsername(username);
    }
}
