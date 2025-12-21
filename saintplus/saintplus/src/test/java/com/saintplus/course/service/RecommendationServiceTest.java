package com.saintplus.course.service;

import com.saintplus.course.domain.Course;
import com.saintplus.course.dto.AiRecommendRequest;
import com.saintplus.course.dto.AiRecommendResponse;
import com.saintplus.course.dto.AiRecommendationDto;
import com.saintplus.course.dto.RecommendedCourseDto;
import com.saintplus.transcript.domain.Enrollment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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



    @Test
    @DisplayName("AI 서버 추천 결과 필터링 및 상위 5개 추출 통합 테스트")
    void testAiRecommendationLogic() {
        // 1. 환경 설정
        String pythonServerUrl = "http://3.39.70.109:8000/recommend";
        RestTemplate restTemplate = new RestTemplate();

        // 가상의 수강 완료 과목 (이 과목들은 결과에서 제외되어야 함)
        Set<String> takenCourseCodes = Set.of("CSE4200");

        // 2. 요청 데이터 생성
        AiRecommendRequest request = new AiRecommendRequest("데이터 분석 관련 과목 추천해줘", "CSE", 0.1);

        try {
            // 3. AI 서버 통신 (Step 1)
            //System.out.println(">>> AI 서버에 요청 중: " + pythonServerUrl);
            AiRecommendResponse response = restTemplate.postForObject(pythonServerUrl, request, AiRecommendResponse.class);

            assertThat(response).isNotNull();
            assertThat(response.getResults()).isNotNull();
            System.out.println(">>> 서버로부터 받은 원본 과목 수: " + response.getResults().size());

            List<AiRecommendResponse.AiCourseItem> results = response.getResults();

            System.out.println("===== 수강한 과목을 제외하지 않은 리스트 3개 =====");
            for (int i = 0; i < 3; i++) {
                AiRecommendResponse.AiCourseItem item = results.get(i);
                System.out.println("과목 코드: " + item.getCode() + "   유사도 점수: " + item.getScore());
            }


            // 4. 필터링, 정렬, 상위 5개 추출 로직 실행 (Step 2)
            List<RecommendedCourseDto> finalRecommendations = response.getResults().stream()
                    // (1) 사용자가 이미 수강한 과목 제외
                    .filter(item -> !takenCourseCodes.contains(item.getCode()))
                    // (2) 유사도 점수 기준 내림차순 정렬
                    .sorted(Comparator.comparingDouble(AiRecommendResponse.AiCourseItem::getScore).reversed())
                    // (3) 상위 5개만 추출
                    .limit(3)
                    // (4) DTO로 변환
                    .map(item -> RecommendedCourseDto.builder()
                            .score(item.getScore())
                            .course(new Course(item.getCode(), "임시 과목명")) // 실제로는 Repository 조회 필요
                            .build())
                    .collect(Collectors.toList());

            // 5. 결과 검증 및 출력 (Step 3)
            System.out.println("\n====== 수강한 과목을 뺀 리스트 3개 ======");
            if (finalRecommendations.isEmpty()) {
                System.out.println("추천 결과가 없습니다.");
            } else {
                for (int i = 0; i < finalRecommendations.size(); i++) {
                    RecommendedCourseDto dto = finalRecommendations.get(i);
                    assertThat(dto.getCourse().getCourseCode()).isNotEqualTo("CSE4020");
                    System.out.printf("[%d] 과목코드: %s | 유사도: %.4f\n",
                            i + 1, dto.getCourse().getCourseCode(), dto.getScore());
                }
            }

            // 검증 로직
            assertThat(finalRecommendations.size()).isLessThanOrEqualTo(5);
            assertThat(finalRecommendations).extracting(r -> r.getCourse().getCourseCode())
                    .doesNotContainAnyElementsOf(takenCourseCodes); // 이미 들은 과목이 없는지 검증

        } catch (Exception e) {
            System.err.println("테스트 중 오류 발생: " + e.getMessage());
            throw e;
        }
    }
}
