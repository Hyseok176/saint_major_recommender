package com.saintplus.course.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class AiRecommendationDto {
    private Long user_id;
    private List<String> recommendations; // Python이 주는 과목 리스트
}