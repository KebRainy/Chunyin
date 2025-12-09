package com.example.demo1.service;

import com.example.demo1.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    @Value("${app.storage.upload-dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("不能为空文件");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException("文件类型不支持");
        }
        try {
            Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (extension != null ? "." + extension : "");
            Path target = directory.resolve(filename);
            Files.copy(file.getInputStream(), target);
            return "/files/" + filename;
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }
    }
}
