package com.example.course_analyzer;

import lombok.Data;
import java.util.List;

@Data
public class RecommendationRequestDto {
    private List<String> cartCourseCodes;
    private List<String> dismissedCourseCodes;
}
