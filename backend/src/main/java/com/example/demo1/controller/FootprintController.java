package com.example.demo1.controller;

import com.example.demo1.common.enums.CollectionTargetType;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.FootprintRecordRequest;
import com.example.demo1.dto.response.FootprintVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.FootprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/footprints")
@RequiredArgsConstructor
public class FootprintController {

    private final FootprintService footprintService;

    @PostMapping
    public Result<Void> record(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody FootprintRecordRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        footprintService.record(principal.getId(),
            request.getTargetType(),
            request.getTargetId(),
            request.getTitle(),
            request.getSummary(),
            request.getCoverUrl());
        return Result.success();
    }

    @GetMapping
    public Result<List<FootprintVO>> list(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam(defaultValue = "7") Integer days,
                                          @RequestParam(required = false) CollectionTargetType type) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        int window = Math.min(Math.max(days, 1), 7);
        return Result.success(footprintService.listRecent(principal.getId(), window, type));
    }
}

