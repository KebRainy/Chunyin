package com.example.demo1.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.response.BeverageSummaryVO;
import com.example.demo1.dto.response.SearchResultVO;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.entity.Beverage;
import com.example.demo1.mapper.BeverageMapper;
import com.example.demo1.service.SharePostService;
import com.example.demo1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SharePostService sharePostService;
    private final UserService userService;
    private final BeverageMapper beverageMapper;

    @GetMapping
    public Result<SearchResultVO> search(@RequestParam String keyword,
                                         @RequestParam(required = false, defaultValue = "all") String type) {
        SearchResultVO.SearchResultVOBuilder builder = SearchResultVO.builder();
        String normalized = keyword == null ? "" : keyword.trim();

        if ("all".equalsIgnoreCase(type) || "circle".equalsIgnoreCase(type)) {
            List<SharePostVO> posts = sharePostService.search(normalized, 10);
            builder.posts(posts);
        }
        if ("all".equalsIgnoreCase(type) || "beverage".equalsIgnoreCase(type)) {
            builder.beverages(searchBeverages(normalized));
        }
        if ("all".equalsIgnoreCase(type) || "user".equalsIgnoreCase(type)) {
            List<SimpleUserVO> users = userService.searchUsers(normalized, 10);
            builder.users(users);
        }
        return Result.success(builder.build());
    }

    private List<BeverageSummaryVO> searchBeverages(String keyword) {
        LambdaQueryWrapper<Beverage> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isBlank(keyword)) {
            wrapper.orderByDesc(Beverage::getCreatedAt);
        } else {
            wrapper.like(Beverage::getName, keyword)
                .or()
                .like(Beverage::getType, keyword);
        }
        wrapper.last("limit 10");
        return beverageMapper.selectList(wrapper).stream()
            .map(beverage -> BeverageSummaryVO.builder()
                .id(beverage.getId())
                .name(beverage.getName())
                .type(beverage.getType())
                .origin(beverage.getOrigin())
                .coverImageId(beverage.getCoverImageId())
                .rating(beverage.getRating())
                .build())
            .collect(Collectors.toList());
    }
}

