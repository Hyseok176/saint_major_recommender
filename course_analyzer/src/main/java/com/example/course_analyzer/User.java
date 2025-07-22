package com.example.course_analyzer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table; // Table 어노테이션 임포트
import lombok.Data;

@Entity
@Data
@Table(name = "users") // 테이블 이름을 "users"로 명시적으로 지정
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
}