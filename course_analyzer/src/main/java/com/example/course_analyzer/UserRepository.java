package com.example.course_analyzer;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, String> {
    @Query("SELECT MAX(u.userOrder) FROM User u")
    Long findMaxUserOrder();
    
}
