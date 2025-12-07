package com.saintplus.course.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecommendationRequestDto {
    private List<String> cartCourseCodes;
    private List<String> dismissedCourseCodes;
    private Integer semester; // 1 or 2
}