package com.saintplus.course.dto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;


@Getter @Setter
public class AiRecommendResponse {
    private List<AiCourseItem> results;

    @Getter @Setter
    public static class AiCourseItem {
        private String code;  // 과목 코드
        private double score; // 유사도 점수
    }
}