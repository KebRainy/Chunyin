package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.enums.FileCategory;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.entity.Image;
import com.example.demo1.mapper.ImageMapper;
import com.example.demo1.repository.ImageDataRepository;
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
    private final ImageDataRepository imageDataRepository;
    private final FileUrlResolver fileUrlResolver;
    private final ContentModerationService contentModerationService;

    public String storeImage(MultipartFile file, Long userId) {
        return storeImage(file, userId, FileCategory.GENERAL);
    }

    public String storeImage(MultipartFile file, Long userId, FileCategory category) {
        validateFile(file);
        try {
            String uuid = UUID.randomUUID().toString();
            byte[] imageData = file.getBytes();

            FileCategory resolvedCategory = FileCategory.fromNullable(category);

            // 图片审核：仅对动态/Wiki等可公开传播的图片做审核
            if (resolvedCategory == FileCategory.POST || resolvedCategory == FileCategory.WIKI) {
                ContentModerationService.ModerationResult moderation =
                    contentModerationService.moderateImage(imageData, file.getContentType(), "UPLOAD_" + resolvedCategory.name());
                if (!moderation.isApproved()) {
                    throw new BusinessException(400, "图片疑似包含违规内容，上传失败");
                }
            }

            Image image = new Image();
            image.setUuid(uuid);
            image.setFileName(file.getOriginalFilename());
            image.setMimeType(file.getContentType());
            image.setFileSize((int) file.getSize());
            image.setUploadedBy(userId);
            image.setCategory(resolvedCategory);

            imageMapper.insert(image);
            imageDataRepository.save(image.getId(), resolvedCategory, imageData);
            return fileUrlResolver.resolve(uuid);
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    public Map<String, Object> storeImageAndGetInfo(MultipartFile file, Long userId) {
        return storeImageAndGetInfo(file, userId, FileCategory.GENERAL);
    }

    public Map<String, Object> storeImageAndGetInfo(MultipartFile file, Long userId, FileCategory category) {
        validateFile(file);

        try {
            String uuid = UUID.randomUUID().toString();
            byte[] imageData = file.getBytes();
            FileCategory resolvedCategory = FileCategory.fromNullable(category);

            // 图片审核：仅对动态/Wiki等可公开传播的图片做审核
            if (resolvedCategory == FileCategory.POST || resolvedCategory == FileCategory.WIKI) {
                ContentModerationService.ModerationResult moderation =
                    contentModerationService.moderateImage(imageData, file.getContentType(), "UPLOAD_" + resolvedCategory.name());
                if (!moderation.isApproved()) {
                    throw new BusinessException(400, "图片疑似包含违规内容，上传失败");
                }
            }

            Image image = new Image();
            image.setUuid(uuid);
            image.setFileName(file.getOriginalFilename());
            image.setMimeType(file.getContentType());
            image.setFileSize((int) file.getSize());
            image.setUploadedBy(userId);
            image.setCategory(resolvedCategory);

            imageMapper.insert(image);
            imageDataRepository.save(image.getId(), resolvedCategory, imageData);
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

    public byte[] loadData(Image image) {
        return imageDataRepository.load(image.getId(), FileCategory.fromNullable(image.getCategory()))
            .orElseThrow(() -> new BusinessException(404, "图片数据不存在"));
    }

    public Image getImageById(Long imageId) {
        return imageMapper.selectById(imageId);
    }

    public void deleteImage(Long imageId) {
        if (imageId == null) {
            return;
        }
        Image image = imageMapper.selectById(imageId);
        deleteImage(image);
    }

    public void deleteImage(Image image) {
        if (image == null || image.getId() == null) {
            return;
        }
        FileCategory category = FileCategory.fromNullable(image.getCategory());
        imageDataRepository.delete(image.getId(), category);
        imageMapper.deleteById(image.getId());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("不能为空文件");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException("文件类型不支持");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过5MB");
        }
    }
}
