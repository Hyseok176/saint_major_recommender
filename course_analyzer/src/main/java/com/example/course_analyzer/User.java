package com.example.course_analyzer;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column
    private String password;

        @Column
    private String nickname; // The display name for the user

    @Column(unique = true)
    private String email;

    private String provider;
    private String providerId;

    private String major1;
    private String major2;
    private String major3;

    private String lastSemester;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // Timestamp for registration order

    @Builder
    public User(String username, String password, String nickname, String email, String provider, String providerId, String major1, String major2, String major3, String lastSemester) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.major1 = major1;
        this.major2 = major2;
        this.major3 = major3;
        this.lastSemester = lastSemester;
    }

    public User update(String nickname) {
        this.nickname = nickname;
        return this;
    }
}