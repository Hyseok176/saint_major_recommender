package com.example.course_analyzer.service;

import com.example.course_analyzer.domain.CourseMapping;
import com.example.course_analyzer.repository.CourseMappingRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataLoader implements CommandLineRunner {

    private final CourseMappingRepository courseMappingRepository;

    public DataLoader(CourseMappingRepository courseMappingRepository) {
        this.courseMappingRepository = courseMappingRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // DB에 데이터가 이미 있는지 확인하여, 중복 로딩 방지
        if (courseMappingRepository.count() > 0) {
            System.out.println("Course data already loaded. Skipping CSV loading.");
            return;
        }

        System.out.println("Loading course data from CSV files...");

        Map<String, CourseMapping> courses = new HashMap<>();

        // 1. 1학기.csv 파일 로드
        loadCoursesFromFile("data/semester1.csv", 1, courses);

        // 2. 2학기.csv 파일 로드
        loadCoursesFromFile("data/semester2.csv", 2, courses);

        // 3. DB에 최종 결과 저장
        courseMappingRepository.saveAll(courses.values());

        System.out.println("Finished loading " + courses.size() + " courses.");
    }

    private void loadCoursesFromFile(String filePath, int semester, Map<String, CourseMapping> courses) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource(filePath).getInputStream(), StandardCharsets.UTF_8))) {
            
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                // CSV 따옴표 필드를 고려한 파싱 로직
                List<String> columns = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                boolean inQuotes = false;
                for (char c : line.toCharArray()) {
                    if (c == '"') {
                        inQuotes = !inQuotes;
                    } else if (c == ',' && !inQuotes) {
                        columns.add(sb.toString().trim());
                        sb.setLength(0);
                    } else {
                        sb.append(c);
                    }
                }
                columns.add(sb.toString().trim());

                if (columns.size() > 6) {
                    String courseCode = columns.get(4);
                    String courseName = columns.get(6);

                    // 파싱 과정에서 추가될 수 있는 불필요한 따옴표 제거
                    if (courseName.startsWith("\"") && courseName.endsWith("\"")) {
                        courseName = courseName.substring(1, courseName.length() - 1);
                    }
                    
                    if (courseCode.isEmpty()) {
                        continue;
                    }

                    CourseMapping course = courses.get(courseCode);
                    if (course != null) {
                        if (course.getSemester() == 1 && semester == 2) {
                            course.setSemester(3);
                        }
                    } else {
                        CourseMapping newCourse = new CourseMapping();
                        newCourse.setCourseCode(courseCode);
                        newCourse.setCourseName(courseName);
                        newCourse.setSemester(semester);
                        courses.put(courseCode, newCourse);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while loading " + filePath);
            e.printStackTrace();
        }
    }
}
