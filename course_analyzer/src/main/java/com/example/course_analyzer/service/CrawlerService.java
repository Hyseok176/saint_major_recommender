package com.example.course_analyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.Month;

@Service
public class CrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    @Scheduled(cron = "0 0 13 * * *") // 매일 오후 1시에 실행
    public void runCrawler() {
        LocalDate now = LocalDate.now();
        // 파이썬 스크립트는 '학년도' 없이 숫자만 받으므로 String.valueOf() 사용
        String yearToSelect = String.valueOf(now.getYear()); 
        
        Month month = now.getMonth();
        String semesterToSelect;

        // 1월~6월: 1학기, 7월~12월: 2학기
        if (month.getValue() >= 1 && month.getValue() <= 6) {
            semesterToSelect = "1학기";
        } else {
            semesterToSelect = "2학기";
        }
        
        executeCrawler(yearToSelect, semesterToSelect);
    }

    public void runCrawlerImmediately(String year, String semester) {
        logger.info("Manual trigger for crawler execution with params: {} {}", year, semester);
        executeCrawler(year, semester);
    }

    private void executeCrawler(String yearToSelect, String semesterToSelect) {
        try {
            logger.info("Starting crawler for year {} and semester {}", yearToSelect, semesterToSelect);

            // 다운로드 경로 설정 (src/main/resources/data)
            String downloadPath = "course_analyzer/src/main/resources/data";
            File downloadDir = new File(downloadPath);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            logger.info("Download path set to: " + downloadDir.getAbsolutePath());

            String pythonScriptPath = "sogang_crawler_gui.py"; // 호출할 스크립트 이름 변경
            ProcessBuilder processBuilder = new ProcessBuilder("python", pythonScriptPath, yearToSelect, semesterToSelect, downloadDir.getAbsolutePath());
            processBuilder.directory(new File("..")); // 작업 디렉토리를 상위 폴더(프로젝트 루트)로 설정
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[Crawler Output] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Crawler script executed successfully.");
            } else {
                logger.error("Crawler script execution failed with exit code: " + exitCode);
            }

        } catch (Exception e) {
            logger.error("An error occurred while running the crawler script.", e);
        }
    }
}
