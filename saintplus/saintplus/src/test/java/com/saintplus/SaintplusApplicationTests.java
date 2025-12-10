package com.saintplus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot 애플리케이션 통합 테스트
 * 
 * 애플리케이션의 전반적인 컨텍스트 로드 및 주요 Bean들의 정상 작동을 확인합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class SaintplusApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@DisplayName("Spring Context 로드 테스트")
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
	}

	@Test
	@DisplayName("주요 Bean 로드 확인 - CourseService")
	void courseServiceBeanLoads() {
		assertThat(applicationContext.containsBean("courseService")).isTrue();
	}

	@Test
	@DisplayName("주요 Bean 로드 확인 - UserService")
	void userServiceBeanLoads() {
		assertThat(applicationContext.containsBean("userService")).isTrue();
	}

	@Test
	@DisplayName("주요 Bean 로드 확인 - TranscriptService")
	void transcriptServiceBeanLoads() {
		assertThat(applicationContext.containsBean("transcriptService")).isTrue();
	}

}
