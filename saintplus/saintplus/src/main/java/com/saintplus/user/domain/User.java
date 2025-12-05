package com.saintplus.user.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String provider;
    private String providerId;


    @Column
    private String nickname; // The display name for the user

    @Setter
    private String lastSemester;

    @Setter
    private String major1;
    @Setter
    private String major2;
    @Setter
    private String major3;


    @Column
    @Setter
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


    public User updateNickname(String nickname) {
        this.nickname = nickname;
        return this;
    }



}