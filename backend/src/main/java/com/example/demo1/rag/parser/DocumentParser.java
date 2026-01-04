package com.example.demo1.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析器
 * 支持PDF、TXT、Markdown等格式
 */
@Component
@Slf4j
public class DocumentParser {
    
    private final Tika tika = new Tika();
    
    /**
     * 解析文档内容
     */
    public String parseDocument(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        String extension = getFileExtension(fileName).toLowerCase();
        
        try (InputStream inputStream = file.getInputStream()) {
            switch (extension) {
                case "txt":
                case "md":
                case "markdown":
                    return parseTextFile(inputStream, file);
                case "pdf":
                    return parsePdfFile(inputStream);
                default:
                    // 使用Tika自动检测
                    return tika.parseToString(inputStream);
            }
        } catch (TikaException e) {
            log.error("解析文档失败: {}", fileName, e);
            throw new IOException("文档解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析文本文件（TXT/MD）
     */
    private String parseTextFile(InputStream inputStream, MultipartFile file) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 解析PDF文件
     */
    private String parsePdfFile(InputStream inputStream) throws IOException, TikaException {
        return tika.parseToString(inputStream);
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 检查文件类型是否支持
     */
    public boolean isSupportedFileType(String fileName) {
        if (fileName == null) {
            return false;
        }
        
        String extension = getFileExtension(fileName).toLowerCase();
        return extension.equals("txt") 
                || extension.equals("md") 
                || extension.equals("markdown")
                || extension.equals("pdf");
    }
}


