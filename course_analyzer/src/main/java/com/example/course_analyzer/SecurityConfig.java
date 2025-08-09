package com.example.course_analyzer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 개발 환경용 보안 설정
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/", "/register", "/images/**", "/extract-majors").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/results", true)
                        .failureUrl("/?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                // H2 Console 및 /extract-majors 경로에 대한 CSRF 보호 비활성화
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/extract-majors"))
                // H2 Console이 프레임 안에서 작동하도록 설정
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    // 운영 환경용 보안 설정
    @Bean
    @Profile("!dev") // dev 프로파일이 아닐 때 활성화
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 운영 환경에서는 H2 콘솔 접근을 허용하지 않음
                        .requestMatchers("/", "/register", "/images/**", "/extract-majors").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/results", true)
                        .failureUrl("/?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                // /extract-majors 경로에 대한 CSRF 보호 비활성화
                .csrf(csrf -> csrf.ignoringRequestMatchers("/extract-majors"));

        return http.build();
    }
}
