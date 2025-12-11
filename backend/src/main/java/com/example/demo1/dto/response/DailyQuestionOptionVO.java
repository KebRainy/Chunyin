package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyQuestionOptionVO {
    private int index;
    private String label;
    private String text;
    private int count;
    private double percentage;
}
