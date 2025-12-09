package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.WikiPageRequest;
import com.example.demo1.dto.response.WikiPageVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.WikiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wiki/pages")
@RequiredArgsConstructor
public class WikiController {

    private final WikiService wikiService;

    @GetMapping
    public Result<PageResult<WikiPageVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(wikiService.listPages(keyword, page, pageSize));
    }

    @GetMapping("/{slug}")
    public Result<WikiPageVO> detail(@PathVariable String slug) {
        return Result.success(wikiService.getPage(slug));
    }

    @PostMapping
    public Result<WikiPageVO> create(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody WikiPageRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(wikiService.createPage(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public Result<WikiPageVO> update(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody WikiPageRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(wikiService.updatePage(id, principal.getId(), request));
    }
}


