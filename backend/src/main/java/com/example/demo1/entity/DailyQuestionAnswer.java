package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("daily_question_answer")
public class DailyQuestionAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;
    private Long userId;
    private Integer selectedOption;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
