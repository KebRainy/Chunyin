package com.example.demo1.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.response.Result;
import com.example.demo1.entity.Alcohol;
import com.example.demo1.mapper.AlcoholMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 酒类标签控制器
 */
@RestController
@RequestMapping("/alcohols")
@RequiredArgsConstructor
public class AlcoholController {

    private final AlcoholMapper alcoholMapper;

    /**
     * 获取所有酒类标签列表
     */
    @GetMapping
    public Result<List<Alcohol>> getAlcoholList() {
        LambdaQueryWrapper<Alcohol> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Alcohol::getName);
        List<Alcohol> alcohols = alcoholMapper.selectList(wrapper);
        return Result.success(alcohols);
    }
}

