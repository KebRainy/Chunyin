package com.example.demo1.controller;

import com.example.demo1.common.enums.TimeDimension;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.SharePostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 排行榜控制器
 * 提供热门动态排行榜等功能
 */
@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final SharePostService sharePostService;

    /**
     * 获取热门动态排行榜
     * 
     * @param timeDimension 时间维度（DAY/WEEK/MONTH/ALL），默认WEEK
     * @param page 页码，默认1
     * @param pageSize 每页大小，默认12
     * @param principal 当前登录用户
     * @return 热门动态分页列表
     */
    @GetMapping("/hot")
    public Result<PageResult<SharePostVO>> getHotPosts(
            @RequestParam(defaultValue = "WEEK") String timeDimension,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        // 解析时间维度
        TimeDimension dimension;
        try {
            dimension = TimeDimension.valueOf(timeDimension.toUpperCase());
        } catch (IllegalArgumentException e) {
            dimension = TimeDimension.WEEK; // 默认使用本周
        }
        
        Long currentUserId = principal != null ? principal.getId() : null;
        PageResult<SharePostVO> result = sharePostService.getHotPosts(dimension, page, pageSize, currentUserId);
        
        return Result.success(result);
    }
}

