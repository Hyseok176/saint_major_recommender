package com.example.course_analyzer.controller;

import com.example.course_analyzer.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AdminController
 *
 * 관리자 기능 및 시스템 유틸리티와 관련된 요청을 처리하는 컨트롤러입니다.
 * 현재는 주로 크롤러 관리 기능을 담당합니다.
 *
 * 주요 기능:
 * 1. 크롤러 관리자 페이지
 * 2. 크롤러 수동 실행 테스트
 * 3. 크롤링된 파일 목록 조회
 */
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final CrawlerService crawlerService;

    /**
     * 크롤러 관리자 페이지를 반환합니다.
     *
     * URL: /crawler-admin
     *
     * @return crawler-admin.html 템플릿 이름
     */
    @GetMapping("/crawler-admin")
    public String crawlerAdminPage() {
        return "crawler-admin";
    }

    /**
     * 크롤러를 수동으로 실행합니다. (테스트 용도)
     *
     * URL: /test-crawler
     *
     * @param year 크롤링할 연도
     * @param semester 크롤링할 학기 (1, 2, summer, winter)
     * @return 실행 결과 메시지
     */
    @GetMapping("/test-crawler")
    @ResponseBody
    public ResponseEntity<String> testCrawler(@RequestParam("year") String year, @RequestParam("semester") String semester) {
        try {
            String semesterInKorean;
            // 파라미터 값을 파이썬 스크립트가 이해하는 한글 학기명으로 변환
            switch (semester) {
                case "1":
                    semesterInKorean = "1학기";
                    break;
                case "2":
                    semesterInKorean = "2학기";
                    break;
                case "summer":
                    semesterInKorean = "하계학기";
                    break;
                case "winter":
                    semesterInKorean = "동계학기";
                    break;
                default:
                    return ResponseEntity.badRequest().body("Invalid semester value: " + semester);
            }

            // 크롤러 서비스 실행
            crawlerService.runCrawlerImmediately(year, semesterInKorean);
            return ResponseEntity.ok("Crawler execution started for " + year + " " + semester + ". Check server logs.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during crawler execution: " + e.getMessage());
        }
    }

    /**
     * 서버에 저장된 크롤링 데이터 파일(.csv) 목록을 조회합니다.
     *
     * URL: /api/crawled-files
     *
     * @return 파일 이름 리스트
     */
    @GetMapping("/api/crawled-files")
    @ResponseBody
    public ResponseEntity<List<String>> getCrawledFiles() {
        // TODO: 경로를 설정 파일(application.properties)로 분리하는 것이 좋음
        File dataDirectory = new File("course_analyzer/src/main/resources/data");
        
        if (!dataDirectory.exists() || !dataDirectory.isDirectory()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        String[] files = dataDirectory.list((dir, name) -> name.toLowerCase().endsWith(".csv"));
        List<String> fileList = (files != null) ? Arrays.asList(files) : new ArrayList<>();

        return ResponseEntity.ok(fileList);
    }
}
