package com.saintplus.course.service;

import com.saintplus.course.dto.AiRecommendRequest;
import com.saintplus.course.dto.AiRecommendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class RecommendationServiceTest {

    @Test
    @DisplayName("파이썬 서버에 프롬포트를 보내고 과목코드와 유사도 받기")
    void testPythonServerConnect(){
        String pythonServerUrl = "http://3.39.70.109:8000/recommend";

        // 실제 보낼 데이터 생성
        String prompt = "데이터 분석과 관련된 기초 과목을 추천해줘";
        String targetMajor = "CSE"; // 컴퓨터공학
        double threshold = 0.2;     // 유사도 기준

        AiRecommendRequest request = new AiRecommendRequest(prompt, targetMajor, threshold);
        // 통신을 위한 RestTemplate 생성
        RestTemplate restTemplate = new RestTemplate();

        // 2. 실행 (When)
        System.out.println("====== 요청 시작: " + pythonServerUrl + " ======");
        System.out.println("보내는 프롬프트: " + request.getPrompt());

        try {
            // POST 요청 전송 및 응답 수신
            AiRecommendResponse response = restTemplate.postForObject(
                    pythonServerUrl,
                    request,
                    AiRecommendResponse.class
            );

            // 3. 검증 (Then)
            System.out.println("====== 응답 도착 ======");

            // 응답이 null이 아닌지 확인

            assertThat(response).isNotNull();
            assertThat(response.getResults()).isNotNull();

            // 콘솔에 결과 출력 (눈으로 확인용)
            if (response.getResults().isEmpty()) {
                System.out.println("결과: 추천된 과목이 없습니다. (유사도 점수가 낮거나 데이터가 없을 수 있음)");
            } else {
                System.out.println("추천된 과목 수: " + response.getResults().size());
                for (AiRecommendResponse.AiCourseItem item : response.getResults()) {
                    System.out.println("--------------------------------");
                    System.out.println("과목 코드: " + item.getCode());
                    System.out.println("유사도 점수: " + item.getScore());
                }
            }

        } catch (Exception e) {
            // 연결 실패 시 에러 메시지 출력
            System.err.println("!!! 파이썬 서버 연결 실패 !!!");
            System.err.println("에러 메시지: " + e.getMessage());
            System.err.println("1. 파이썬 서버가 켜져 있는지(localhost:8000) 확인하세요.");
            System.err.println("2. URL 경로(/recommend)가 정확한지 확인하세요.");
            // 테스트 실패 처리
            throw e;
        }
    }
}
