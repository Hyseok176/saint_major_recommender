package com.saintplus.transcript.service;

import com.saintplus.course.dto.CourseAnalysisData;
import com.saintplus.transcript.dto.TranscriptParsingResult;
import com.saintplus.transcript.dto.TranscriptScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptParserTest {

    private TranscriptParser transcriptParser;

    @BeforeEach
    void setUp() {
        transcriptParser = new TranscriptParser();
    }

    @Test
    @DisplayName("analyzeFile() 테스트")
    void analyzeFile_realFile() throws Exception {

        // 테스트 파일
        ClassPathResource file =
                new ClassPathResource("하영욱.txt");
        InputStream inputStream = file.getInputStream();

        TranscriptScanResult scanResult = transcriptParser.analyzeFile(inputStream, "101");
        List<CourseAnalysisData> rawCourses = scanResult.getRawCourses();
        Map<String,String> mapping = scanResult.getMappingCourseCodeName();


        // 파싱된 강의 개수 확인
        //assertThat(rawCourses).hasSize(17);
        //assertThat(mapping).hasSize(17);


        // 패턴 예외 없는지 확인
        for(int i = (Math.max(rawCourses.size()-30, 0)); i< rawCourses.size(); i++){
            System.out.print(rawCourses.get(i).getSemester() + "  ");
            System.out.print(rawCourses.get(i).getCourseCode() + "  ");
            System.out.print(rawCourses.get(i).getCourseName() + "  ");
            System.out.print(rawCourses.get(i).getImportantRemarks().isFailed()+",");
            System.out.print(rawCourses.get(i).getImportantRemarks().isEnglishLecture()+",");
            System.out.println(rawCourses.get(i).getImportantRemarks().isRetake());
        }


        // 과목 코드 및 이름 매핑 다 잘 저장되었는지
        for(int i=0;i< rawCourses.size();i++){
            assertThat(mapping).containsKey(rawCourses.get(i).getCourseCode());
        }


    }



    @Test
    void groupAndFormatCourses_realFile() throws Exception{

        // 테스트 파일
        ClassPathResource file =
                new ClassPathResource("하영욱.txt");
        InputStream inputStream = file.getInputStream();

        TranscriptScanResult scanResult = transcriptParser.analyzeFile(inputStream, "101");
        TranscriptParser parser = new TranscriptParser();

        // 분류 및 정렬
        TranscriptParsingResult parsingResult = parser.groupAndFormatCourses(scanResult.getRawCourses());

        Map<String, List<CourseAnalysisData>> grouped = parsingResult.getCoursesBySemester();

        // then 1) 중복 수강 남아 있는지 (2개)
        long countECO2002 = grouped.values().stream()
                .flatMap(List::stream)
                .filter(c -> c.getCourseCode().equals("ECO2002"))
                .count();

        assertThat(countECO2002).isEqualTo(2);

        // then 2) 학기 key 생성 확인 (정확한 정렬)
        System.out.println(grouped.keySet());

        // then 3) 과목 코드 순 정렬 확인 (학기 특정 그룹에서)
        List<CourseAnalysisData> sem22_2 = grouped.get("2학기 (2022년 2학기)");
        for(int i=0;i<sem22_2.size();i++){
            System.out.println(sem22_2.get(i).getCourseCode());
        }

        // then 4) 마지막 학기 문자열 확인
        assertThat(parsingResult.getLastSemester()).isEqualTo("2025-2");
    }



    @Test
    @DisplayName("extractMajors() 테스트")
    void extractMajors_realFileWithClassPathResource() throws Exception {

        ClassPathResource resource =
                new ClassPathResource("하영욱.txt");

        InputStream inputStream = resource.getInputStream();

        List<String> majors = transcriptParser.extractMajorsFromFile(inputStream);

        System.out.println(majors);
    }
}


