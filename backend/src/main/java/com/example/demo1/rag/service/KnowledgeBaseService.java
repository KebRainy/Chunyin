package com.example.demo1.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.entity.Beverage;
import com.example.demo1.entity.BeverageVector;
import com.example.demo1.entity.WikiPage;
import com.example.demo1.mapper.BeverageMapper;
import com.example.demo1.mapper.BeverageVectorMapper;
import com.example.demo1.mapper.WikiPageMapper;
import com.example.demo1.common.enums.WikiStatus;
import com.example.demo1.rag.parser.ChunkingStrategy;
import com.example.demo1.rag.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库构建服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {
    
    @Value("${rag.documents.directory:}")
    private String documentsDirectory;
    
    private final BeverageMapper beverageMapper;
    private final BeverageVectorMapper beverageVectorMapper;
    private final WikiPageMapper wikiPageMapper;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final DocumentParser documentParser;
    private final ChunkingStrategy chunkingStrategy;
    
    /**
     * 从数据库提取酒类信息并向量化
     */
    @Transactional
    public void buildKnowledgeBaseFromDatabase() {
        log.info("开始构建知识库...");
        
        // 获取所有活跃的酒类
        List<Beverage> beverages = beverageMapper.selectList(
                new LambdaQueryWrapper<Beverage>()
                        .eq(Beverage::getIsActive, true)
        );
        
        log.info("找到 {} 个酒类，开始向量化...", beverages.size());
        
        List<Long> beverageIds = new ArrayList<>();
        List<List<Float>> vectors = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        
        for (Beverage beverage : beverages) {
            // 构建文本内容
            String text = buildBeverageText(beverage);
            texts.add(text);
            
            // 向量化
            List<Float> vector = embeddingService.embedText(text);
            if (!vector.isEmpty()) {
                beverageIds.add(beverage.getId());
                vectors.add(vector);
            }
        }
        
        // 批量插入向量数据库
        if (!beverageIds.isEmpty()) {
            vectorStoreService.insertVectors(beverageIds, vectors);
            
            // 保存到MySQL（用于备份和查询）
            for (int i = 0; i < beverageIds.size(); i++) {
                BeverageVector beverageVector = new BeverageVector();
                beverageVector.setBeverageId(beverageIds.get(i));
                beverageVector.setTextContent(texts.get(i));
                beverageVector.setVector(embeddingService.vectorToJson(vectors.get(i)));
                beverageVector.setSourceType("database");
                beverageVector.setChunkIndex(0);
                
                beverageVectorMapper.insert(beverageVector);
            }
            
            log.info("知识库构建完成，共处理 {} 个酒类", beverageIds.size());
        }
    }
    
    /**
     * 构建酒类文本内容
     */
    private String buildBeverageText(Beverage beverage) {
        StringBuilder text = new StringBuilder();
        
        text.append("酒类名称：").append(beverage.getName()).append("。");
        if (beverage.getNameEn() != null && !beverage.getNameEn().isEmpty()) {
            text.append("英文名：").append(beverage.getNameEn()).append("。");
        }
        text.append("类型：").append(beverage.getType()).append("。");
        
        if (beverage.getOrigin() != null && !beverage.getOrigin().isEmpty()) {
            text.append("产地：").append(beverage.getOrigin()).append("。");
        }
        
        if (beverage.getAlcoholContent() != null) {
            text.append("酒精度：").append(beverage.getAlcoholContent()).append("%。");
        }
        
        if (beverage.getDescription() != null && !beverage.getDescription().isEmpty()) {
            text.append("描述：").append(beverage.getDescription()).append("。");
        }
        
        if (beverage.getIngredients() != null && !beverage.getIngredients().isEmpty()) {
            text.append("成分：").append(beverage.getIngredients()).append("。");
        }
        
        if (beverage.getTasteNotes() != null && !beverage.getTasteNotes().isEmpty()) {
            text.append("口感：").append(beverage.getTasteNotes()).append("。");
        }
        
        if (beverage.getRating() != null) {
            text.append("评分：").append(beverage.getRating()).append("分。");
        }
        
        return text.toString();
    }
    
    /**
     * 增量更新单个酒类的向量
     */
    @Transactional
    public void updateBeverageVector(Long beverageId) {
        Beverage beverage = beverageMapper.selectById(beverageId);
        if (beverage == null || !beverage.getIsActive()) {
            return;
        }
        
        String text = buildBeverageText(beverage);
        List<Float> vector = embeddingService.embedText(text);
        
        if (!vector.isEmpty()) {
            // 更新Milvus（简化实现，实际应该先删除再插入）
            List<Long> ids = new ArrayList<>();
            ids.add(beverageId);
            List<List<Float>> vectors = new ArrayList<>();
            vectors.add(vector);
            vectorStoreService.insertVectors(ids, vectors);
            
            // 更新MySQL
            BeverageVector existing = beverageVectorMapper.selectOne(
                    new LambdaQueryWrapper<BeverageVector>()
                            .eq(BeverageVector::getBeverageId, beverageId)
                            .eq(BeverageVector::getSourceType, "database")
                            .last("limit 1")
            );
            
            if (existing != null) {
                existing.setTextContent(text);
                existing.setVector(embeddingService.vectorToJson(vector));
                beverageVectorMapper.updateById(existing);
            } else {
                BeverageVector beverageVector = new BeverageVector();
                beverageVector.setBeverageId(beverageId);
                beverageVector.setTextContent(text);
                beverageVector.setVector(embeddingService.vectorToJson(vector));
                beverageVector.setSourceType("database");
                beverageVector.setChunkIndex(0);
                beverageVectorMapper.insert(beverageVector);
            }
        }
    }
    
    /**
     * 导入外部文档并向量化
     */
    @Transactional
    public void importExternalDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        String fileName = file.getOriginalFilename();
        if (!documentParser.isSupportedFileType(fileName)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持：TXT、MD、PDF");
        }
        
        log.info("开始导入外部文档: {}", fileName);
        
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
        
        // 4. 存储到Milvus向量数据库（仅存储向量，不存储到MySQL）
        // 过滤有效向量
        List<List<Float>> validVectors = new ArrayList<>();
        
        for (int i = 0; i < vectors.size(); i++) {
            if (i < vectors.size() && !vectors.get(i).isEmpty()) {
                validVectors.add(vectors.get(i));
            }
        }
        
        if (!validVectors.isEmpty()) {
            // 插入Milvus（外部文档使用0作为beverage_id标识）
            vectorStoreService.insertExternalDocumentVectors(validVectors);
            
            log.info("外部文档导入完成，共处理 {} 个文档块，已存储到Milvus", validVectors.size());
        } else {
            log.warn("没有有效的向量数据，文档导入失败");
        }
    }
    
    /**
     * 批量导入外部文档
     */
    @Transactional
    public void importExternalDocuments(List<MultipartFile> files) throws IOException {
        int successCount = 0;
        int failCount = 0;
        
        for (MultipartFile file : files) {
            try {
                importExternalDocument(file);
                successCount++;
            } catch (Exception e) {
                log.error("导入文档失败: {}", file.getOriginalFilename(), e);
                failCount++;
            }
        }
        
            log.info("批量导入完成，成功: {}，失败: {}", successCount, failCount);
        
        if (failCount > 0) {
            throw new IOException(String.format("部分文档导入失败，成功: %d，失败: %d", successCount, failCount));
        }
    }
    
    /**
     * 从服务器文件系统导入文档（批量）
     * @param directoryPath 文档目录路径
     */
    @Transactional
    public void importDocumentsFromDirectory(String directoryPath) throws IOException {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("目录路径不能为空");
        }
        
        java.nio.file.Path dir = java.nio.file.Paths.get(directoryPath);
        if (!java.nio.file.Files.exists(dir) || !java.nio.file.Files.isDirectory(dir)) {
            throw new IllegalArgumentException("目录不存在或不是有效目录: " + directoryPath);
        }
        
        log.info("开始从目录导入文档: {}", directoryPath);
        
        List<java.nio.file.Path> files = java.nio.file.Files.list(dir)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".pdf") || 
                           fileName.endsWith(".txt") || 
                           fileName.endsWith(".md");
                })
                .collect(java.util.stream.Collectors.toList());
        
        if (files.isEmpty()) {
            log.warn("目录中没有找到支持的文档文件: {}", directoryPath);
            return;
        }
        
        log.info("找到 {} 个文档文件，开始导入...", files.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (java.nio.file.Path filePath : files) {
            try {
                // 创建自定义 MultipartFile
                byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);
                String fileName = filePath.getFileName().toString();
                String contentType = java.nio.file.Files.probeContentType(filePath);
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
                
                importExternalDocument(multipartFile);
                successCount++;
                log.info("成功导入文档: {}", fileName);
            } catch (Exception e) {
                failCount++;
                log.error("导入文档失败: {}", filePath.getFileName(), e);
            }
        }
        
        log.info("目录导入完成，成功: {}，失败: {}", successCount, failCount);
        
        if (failCount > 0 && successCount == 0) {
            throw new IOException(String.format("所有文档导入失败，成功: %d，失败: %d", successCount, failCount));
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
            java.nio.file.Files.write(dest.toPath(), content != null ? content : new byte[0]);
        }
    }
    
    /**
     * 从数据库提取Wiki内容并向量化
     */
    @Transactional
    public void buildKnowledgeBaseFromWiki() {
        log.info("开始构建Wiki知识库...");
        
        // 获取所有已发布的Wiki页面
        List<WikiPage> wikiPages = wikiPageMapper.selectList(
                new LambdaQueryWrapper<WikiPage>()
                        .eq(WikiPage::getStatus, WikiStatus.PUBLISHED)
        );
        
        log.info("找到 {} 个Wiki页面，开始向量化...", wikiPages.size());
        
        List<List<Float>> vectors = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        
        for (WikiPage wikiPage : wikiPages) {
            // 构建文本内容
            String text = buildWikiText(wikiPage);
            if (text == null || text.trim().isEmpty()) {
                continue;
            }
            
            // 文档分块（Wiki内容可能较长）
            List<String> chunks = chunkingStrategy.smartChunk(text);
            
            // 批量向量化
            List<List<Float>> chunkVectors = embeddingService.embedTexts(chunks);
            
            for (int i = 0; i < chunks.size(); i++) {
                if (i < chunkVectors.size() && !chunkVectors.get(i).isEmpty()) {
                    texts.add(chunks.get(i));
                    vectors.add(chunkVectors.get(i));
                }
            }
        }
        
        // 批量插入向量数据库
        if (!vectors.isEmpty()) {
            vectorStoreService.insertExternalDocumentVectors(vectors);
            
            // 保存到MySQL（使用source_type='wiki'标识）
            for (int i = 0; i < texts.size(); i++) {
                BeverageVector beverageVector = new BeverageVector();
                beverageVector.setBeverageId(null); // Wiki不关联具体酒类
                beverageVector.setTextContent(texts.get(i));
                beverageVector.setVector(embeddingService.vectorToJson(vectors.get(i)));
                beverageVector.setSourceType("wiki");
                beverageVector.setChunkIndex(i);
                
                beverageVectorMapper.insert(beverageVector);
            }
            
            log.info("Wiki知识库构建完成，共处理 {} 个文档块", texts.size());
        }
    }
    
    /**
     * 构建Wiki文本内容
     */
    private String buildWikiText(WikiPage wikiPage) {
        if (wikiPage == null) {
            return null;
        }
        
        StringBuilder text = new StringBuilder();
        
        text.append("词条标题：").append(wikiPage.getTitle()).append("。");
        
        if (wikiPage.getSummary() != null && !wikiPage.getSummary().isEmpty()) {
            text.append("摘要：").append(wikiPage.getSummary()).append("。");
        }
        
        if (wikiPage.getContent() != null && !wikiPage.getContent().isEmpty()) {
            // 移除Markdown标记，保留纯文本
            String content = wikiPage.getContent()
                    .replaceAll("#+\\s*", "") // 移除标题标记
                    .replaceAll("\\*\\*([^*]+)\\*\\*", "$1") // 移除粗体标记
                    .replaceAll("\\*([^*]+)\\*", "$1") // 移除斜体标记
                    .replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1") // 移除链接标记
                    .replaceAll("```[\\s\\S]*?```", "") // 移除代码块
                    .replaceAll("`([^`]+)`", "$1"); // 移除行内代码
            
            text.append("内容：").append(content);
        }
        
        return text.toString();
    }
}

