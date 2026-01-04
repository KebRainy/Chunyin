package com.example.demo1.rag.util;

import com.example.demo1.rag.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 文档导入工具
 * 用于在系统启动前或独立运行时导入文档
 * 
 * 使用方法：
 * 1. 作为 Spring Boot CommandLineRunner 运行（需要配置）
 * 2. 或通过 API 调用导入
 */
@Component
@Slf4j
public class DocumentImporter implements CommandLineRunner {
    
    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;
    
    @Value("${rag.documents.directory:}")
    private String documentsDirectory;
    
    @Value("${rag.documents.pre-import:false}")
    private boolean preImport;
    
    /**
     * 导入文档目录
     */
    public void importDocuments(String directoryPath) {
        if (knowledgeBaseService == null) {
            log.error("KnowledgeBaseService 未初始化，无法导入文档");
            return;
        }
        
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            log.warn("文档目录路径为空，跳过导入");
            return;
        }
        
        try {
            log.info("开始导入文档目录: {}", directoryPath);
            knowledgeBaseService.importDocumentsFromDirectory(directoryPath);
            log.info("文档导入完成");
        } catch (IOException e) {
            log.error("导入文档失败", e);
            System.exit(1);
        }
    }
    
    /**
     * 命令行运行入口（可选）
     * 通过设置 rag.documents.pre-import=true 启用
     */
    @Override
    public void run(String... args) {
        if (!preImport) {
            log.debug("预导入功能未启用，跳过文档导入");
            return;
        }
        
        if (documentsDirectory == null || documentsDirectory.trim().isEmpty()) {
            log.warn("文档目录未配置，跳过预导入");
            return;
        }
        
        log.info("执行文档预导入...");
        importDocuments(documentsDirectory);
    }
}

