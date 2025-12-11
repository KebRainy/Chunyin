package com.example.demo1.repository;

import com.example.demo1.common.enums.FileCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ImageDataRepository {

    private final JdbcTemplate jdbcTemplate;

    public void save(Long imageId, FileCategory category, byte[] data) {
        String table = tableName(category);
        jdbcTemplate.update("REPLACE INTO " + table + " (image_id, image_data) VALUES (?, ?)", imageId, data);
    }

    public Optional<byte[]> load(Long imageId, FileCategory category) {
        String table = tableName(category);
        return jdbcTemplate.query("SELECT image_data FROM " + table + " WHERE image_id = ?",
            rs -> {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getBytes("image_data"));
                }
                return Optional.empty();
            }, imageId);
    }

    public void delete(Long imageId, FileCategory category) {
        jdbcTemplate.update("DELETE FROM " + tableName(category) + " WHERE image_id = ?", imageId);
    }

    private String tableName(FileCategory category) {
        return switch (FileCategory.fromNullable(category)) {
            case POST -> "post_image_data";
            case AVATAR -> "avatar_image_data";
            case WIKI -> "wiki_image_data";
            default -> "general_image_data";
        };
    }
}
