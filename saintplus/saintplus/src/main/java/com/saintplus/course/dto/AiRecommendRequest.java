package com.saintplus.course.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AiRecommendRequest {
    private String prompt;    // 질문 문장
    private String target;    // 전공 prefix (CSE, MGT 등)
    private double threshold; // 최소 유사도 점수
}

