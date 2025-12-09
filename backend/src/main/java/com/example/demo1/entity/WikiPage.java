package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.demo1.common.enums.WikiStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wiki_page")
public class WikiPage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String slug;
    private String title;
    private String summary;
    private String content;
    private WikiStatus status = WikiStatus.UNDER_REVIEW;
    private Long lastEditorId;
    private String lastEditorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

