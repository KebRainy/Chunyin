package com.example.demo1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DailyQuestionVO {
    private Long id;
    private LocalDate questionDate;
    private String question;
    private List<DailyQuestionOptionVO> options;
    private Integer correctOption;
    private Integer selectedOption;
    private boolean answered;
    private String explanation;
    private String wikiLink;
}
