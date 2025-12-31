package com.example.demo1.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo1.entity.ContentReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容举报Mapper
 */
@Mapper
public interface ContentReportMapper extends BaseMapper<ContentReport> {
}

