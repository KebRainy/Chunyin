package com.example.demo1.rag.tool;

import com.example.demo1.rag.parser.ChunkingStrategy;
import com.example.demo1.rag.parser.DocumentParser;
import com.example.demo1.rag.service.EmbeddingService;
import com.example.demo1.rag.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档嵌入工具
 * 独立工具类，用于将文档嵌入到 Milvus 向量数据库
 * 
 * 注意：此工具不集成到系统 API 中，仅作为独立工具使用
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEmbeddingTool {
    
    private final DocumentParser documentParser;
    private final ChunkingStrategy chunkingStrategy;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    
    /**
     * 将单个文档嵌入到 Milvus
     * 
     * @param file 文档文件
     * @throws IOException 如果文档解析或处理失败
     */
    public void embedDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        String fileName = file.getOriginalFilename();
        if (!documentParser.isSupportedFileType(fileName)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持：TXT、MD、PDF");
        }
        
        log.info("开始处理文档: {}", fileName);
        
        // 1. 解析文档内容
        String content = documentParser.parseDocument(file);
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("文档内容为空");
        }
        
        log.info("文档解析成功，内容长度: {} 字符", content.length());
        
        // 2. 文档分块
        List<String> chunks = chunkingStrategy.smartChunk(content);
        log.info("文档分块完成，共 {} 块", chunks.size());
        
        // 3. 批量向量化
        List<List<Float>> vectors = embeddingService.embedTexts(chunks);
        
        // 4. 过滤有效向量
        List<List<Float>> validVectors = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) {
            if (i < vectors.size() && !vectors.get(i).isEmpty()) {
                validVectors.add(vectors.get(i));
            }
        }
        
        // 5. 存储到 Milvus
        if (!validVectors.isEmpty()) {
            vectorStoreService.insertExternalDocumentVectors(validVectors);
            log.info("文档嵌入完成: {}，共处理 {} 个文档块", fileName, validVectors.size());
        } else {
            log.warn("没有有效的向量数据，文档嵌入失败: {}", fileName);
        }
    }
    
    /**
     * 批量将文档嵌入到 Milvus
     * 
     * @param files 文档文件列表
     * @return 成功和失败的数量
     */
    public EmbeddingResult embedDocuments(List<MultipartFile> files) {
        int successCount = 0;
        int failCount = 0;
        
        for (MultipartFile file : files) {
            try {
                embedDocument(file);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("嵌入文档失败: {}", file.getOriginalFilename(), e);
            }
        }
        
        log.info("批量嵌入完成，成功: {}，失败: {}", successCount, failCount);
        return new EmbeddingResult(successCount, failCount);
    }
    
    /**
     * 从目录读取文档并嵌入到 Milvus
     * 
     * @param directoryPath 文档目录路径
     * @return 嵌入结果
     * @throws IOException 如果目录读取失败
     */
    public EmbeddingResult embedDocumentsFromDirectory(String directoryPath) throws IOException {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("目录路径不能为空");
        }
        
        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("目录不存在或不是有效目录: " + directoryPath);
        }
        
        log.info("开始从目录读取文档: {}", directoryPath);
        
        // 读取目录中的文档文件
        List<Path> files = Files.list(dir)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".pdf") || 
                           fileName.endsWith(".txt") || 
                           fileName.endsWith(".md");
                })
                .collect(Collectors.toList());
        
        if (files.isEmpty()) {
            log.warn("目录中没有找到支持的文档文件: {}", directoryPath);
            return new EmbeddingResult(0, 0);
        }
        
        log.info("找到 {} 个文档文件，开始嵌入...", files.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Path filePath : files) {
            try {
                // 创建自定义 MultipartFile
                byte[] fileBytes = Files.readAllBytes(filePath);
                String fileName = filePath.getFileName().toString();
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    if (fileName.endsWith(".pdf")) {
                        contentType = "application/pdf";
                    } else if (fileName.endsWith(".txt")) {
                        contentType = "text/plain";
                    } else if (fileName.endsWith(".md")) {
                        contentType = "text/markdown";
                    }
                }
                
                MultipartFile multipartFile = new FileSystemMultipartFile(fileName, contentType, fileBytes);
                embedDocument(multipartFile);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("嵌入文档失败: {}", filePath.getFileName(), e);
            }
        }
        
        log.info("目录嵌入完成，成功: {}，失败: {}", successCount, failCount);
        return new EmbeddingResult(successCount, failCount);
    }
    
    /**
     * 嵌入结果
     */
    public static class EmbeddingResult {
        private final int successCount;
        private final int failCount;
        
        public EmbeddingResult(int successCount, int failCount) {
            this.successCount = successCount;
            this.failCount = failCount;
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public int getFailCount() {
            return failCount;
        }
        
        public boolean isAllSuccess() {
            return failCount == 0;
        }
    }
    
    /**
     * 文件系统 MultipartFile 实现
     */
    private static class FileSystemMultipartFile implements MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] content;
        
        public FileSystemMultipartFile(String name, String contentType, byte[] content) {
            this.name = name;
            this.contentType = contentType;
            this.content = content;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getOriginalFilename() {
            return name;
        }
        
        @Override
        public String getContentType() {
            return contentType;
        }
        
        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return content != null ? content : new byte[0];
        }
        
        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new java.io.ByteArrayInputStream(content != null ? content : new byte[0]);
        }
        
        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content != null ? content : new byte[0]);
        }
    }
}

