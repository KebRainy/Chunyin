package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@AuthenticationPrincipal UserPrincipal principal,
                                              @RequestParam("file") MultipartFile file) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        String url = fileStorageService.store(file);
        return Result.success(Map.of("url", url));
    }
}

