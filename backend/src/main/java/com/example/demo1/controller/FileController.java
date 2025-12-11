package com.example.demo1.controller;

import com.example.demo1.common.enums.FileCategory;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.Result;
import com.example.demo1.entity.Image;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@AuthenticationPrincipal UserPrincipal principal,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "category", required = false) FileCategory category) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        Map<String, Object> result = fileStorageService.storeImageAndGetInfo(file, principal.getId(), category);
        return Result.success(result);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<byte[]> getImage(@PathVariable String uuid) {
        Image image = fileStorageService.getImageByUuid(uuid);
        if (image == null) {
            throw new BusinessException(404, "图片不存在");
        }
        byte[] data = fileStorageService.loadData(image);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getMimeType()))
                .contentLength(image.getFileSize())
                .body(data);
    }

    @DeleteMapping("/{uuid}")
    public Result<Void> deleteImage(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable String uuid) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        Image image = fileStorageService.getImageByUuid(uuid);
        if (image == null) {
            throw new BusinessException(404, "图片不存在");
        }
        if (!image.getUploadedBy().equals(principal.getId())) {
            throw new BusinessException(403, "无权删除此图片");
        }
        fileStorageService.deleteImage(image.getId());
        return Result.success();
    }
}

