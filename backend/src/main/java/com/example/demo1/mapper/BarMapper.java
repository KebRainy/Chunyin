package com.example.demo1.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo1.entity.Bar;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BarMapper extends BaseMapper<Bar> {
    // 注意：由于数据库不存储经纬度，附近搜索功能需要通过地理编码API实现
    // findNearbyBars 方法已移除，需要时通过 Service 层调用地理编码API
}

