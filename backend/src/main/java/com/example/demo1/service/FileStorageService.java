package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.entity.Image;
import com.example.demo1.mapper.ImageMapper;
import com.example.demo1.util.FileUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final ImageMapper imageMapper;
    private final FileUrlResolver fileUrlResolver;

    public String storeImage(MultipartFile file, Long userId) {
        if (file.isEmpty()) {
            throw new BusinessException("不能为空文件");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException("文件类型不支持");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过5MB");
        }

        try {
            String uuid = UUID.randomUUID().toString();
            byte[] imageData = file.getBytes();

            Image image = new Image();
            image.setUuid(uuid);
            image.setImageData(imageData);
            image.setFileName(file.getOriginalFilename());
            image.setMimeType(file.getContentType());
            image.setFileSize((int) file.getSize());
            image.setUploadedBy(userId);

            imageMapper.insert(image);
            return fileUrlResolver.resolve(uuid);
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    public Map<String, Object> storeImageAndGetInfo(MultipartFile file, Long userId) {
        if (file.isEmpty()) {
            throw new BusinessException("不能为空文件");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException("文件类型不支持");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过5MB");
        }

        try {
            String uuid = UUID.randomUUID().toString();
            byte[] imageData = file.getBytes();

            Image image = new Image();
            image.setUuid(uuid);
            image.setImageData(imageData);
            image.setFileName(file.getOriginalFilename());
            image.setMimeType(file.getContentType());
            image.setFileSize((int) file.getSize());
            image.setUploadedBy(userId);

            imageMapper.insert(image);
            return Map.of(
                "id", image.getId(),
                "url", fileUrlResolver.resolve(uuid),
                "uuid", uuid
            );
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    public Image getImageByUuid(String uuid) {
        return imageMapper.selectOne(new LambdaQueryWrapper<Image>()
                .eq(Image::getUuid, uuid));
    }

    public Image getImageById(Long imageId) {
        return imageMapper.selectById(imageId);
    }

    public void deleteImage(Long imageId) {
        imageMapper.deleteById(imageId);
    }
}
