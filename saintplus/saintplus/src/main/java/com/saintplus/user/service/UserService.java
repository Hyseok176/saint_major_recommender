package com.saintplus.user.service;

import com.saintplus.user.domain.User;
import com.saintplus.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {

    UserRepository userRepository;

    @Transactional
    public void updateUserData(Long userId, String major1, String major2, String major3) {

        //사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        //사용자 정보 업데이트
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(java.time.LocalDateTime.now());
        }

        user.setMajor1(major1.replace(" ", ""));
        user.setMajor2(major2.replace(" ", ""));
        user.setMajor3(major3.replace(" ", ""));
        userRepository.save(user);

    }

}
