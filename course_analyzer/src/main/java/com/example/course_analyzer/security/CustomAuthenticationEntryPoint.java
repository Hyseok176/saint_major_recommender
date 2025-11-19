package com.example.course_analyzer.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final FlashMapManager flashMapManager = new SessionFlashMapManager();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        FlashMap flashMap = new FlashMap();
        flashMap.put("warning", "로그인이 필요한 서비스입니다.");
        
        // FlashMapManager를 사용하여 다음 요청으로 Flash Attribute를 전달
        flashMapManager.saveOutputFlashMap(flashMap, request, response);
        
        response.sendRedirect("/");
    }
}