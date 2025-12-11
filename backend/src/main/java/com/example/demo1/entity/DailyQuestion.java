package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_question")
public class DailyQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate questionDate;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer correctOption;
    private Integer countA;
    private Integer countB;
    private Integer countC;
    private Integer countD;
    private String explanation;
    private String wikiLink;
    private LocalDateTime createdAt;
}
