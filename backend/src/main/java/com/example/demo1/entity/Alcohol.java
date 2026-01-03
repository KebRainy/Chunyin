package com.example.demo1.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 酒类标签实体
 */
@Data
@TableName("alcohol")
public class Alcohol {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 酒类名称
     */
    private String name;
}

