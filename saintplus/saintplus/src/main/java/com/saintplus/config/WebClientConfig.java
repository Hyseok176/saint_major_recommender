package com.saintplus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient aiWebClient() {
        // 아까 띄운 AWS Python 서버 주소 (http:// 포함 필수)
        return WebClient.builder()
                .baseUrl("http://3.39.70.109:8000")
                .build();
    }
}