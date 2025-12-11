package com.example.demo1.controller;

import com.example.demo1.common.enums.CollectionTargetType;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.response.CollectionItemVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public Result<PageResult<CollectionItemVO>> list(@AuthenticationPrincipal UserPrincipal principal,
                                                     @RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "12") Integer pageSize,
                                                     @RequestParam(required = false) CollectionTargetType type,
                                                     @RequestParam(required = false) String keyword) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(collectionService.listCollections(principal.getId(), type, page, pageSize, keyword));
    }
}

