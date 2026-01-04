package com.example.demo1.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.dto.rag.RAGQueryRequest;
import com.example.demo1.dto.rag.RAGRecommendationResponse;
import com.example.demo1.dto.rag.UserPreferenceVO;
import com.example.demo1.dto.response.BeverageSummaryVO;
import com.example.demo1.dto.response.WikiContentVO;
import com.example.demo1.entity.Beverage;
import com.example.demo1.entity.BeverageVector;
import com.example.demo1.mapper.BeverageMapper;
import com.example.demo1.mapper.BeverageVectorMapper;
import com.example.demo1.rag.client.AliyunLLMClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG推荐服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RAGRecommendationService {
    
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final UserPreferenceService userPreferenceService;
    private final PopularityService popularityService;
    private final AliyunLLMClient llmClient;
    private final BeverageMapper beverageMapper;
    private final BeverageVectorMapper beverageVectorMapper;
    
    /**
     * Wiki内容与相似度的包装类
     */
    private static class WikiContentWithSimilarity {
        BeverageVector beverageVector;
        Double similarity;
        
        WikiContentWithSimilarity(BeverageVector beverageVector, Double similarity) {
            this.beverageVector = beverageVector;
            this.similarity = similarity;
        }
    }
    
    /**
     * RAG智能推荐（同时使用Beverage和Wiki内容）
     */
    public RAGRecommendationResponse recommend(RAGQueryRequest request, Long userId) {
        // 1. 查询理解
        RAGRecommendationResponse.QueryUnderstanding queryUnderstanding = understandQuery(request.getQuery());
        
        // 2. 获取用户偏好
        UserPreferenceVO userPreference = userId != null 
                ? userPreferenceService.extractUserPreference(userId) 
                : null;
        
        // 3. 构建查询向量（融合用户偏好和对话历史）
        List<Float> queryVector = buildQueryVector(request.getQuery(), userPreference, request.getConversationHistory());
        
        // 4. 向量检索（包含数据库酒类和Wiki/外部文档）
        int topK = request.getTopK() != null ? request.getTopK() : 10;
        List<Long> candidateIds = vectorStoreService.searchSimilar(queryVector, topK * 3);
        
        // 分离Beverage ID和Wiki/External ID
        List<Long> validBeverageIds = new ArrayList<>();
        for (Long id : candidateIds) {
            if (id != null && id > 0) {
                validBeverageIds.add(id);
            }
        }
        
        // 5. 处理Beverage内容
        List<BeverageSummaryVO> beverageVOs = new ArrayList<>();
        if (!validBeverageIds.isEmpty()) {
            // 获取候选酒类
            List<Beverage> candidates = beverageMapper.selectBatchIds(validBeverageIds);
            
            // 重排序（结合相似度、用户偏好、热门度）
            List<Beverage> rankedBeverages = rerank(candidates, userPreference, queryVector);
            
            // 取Top-K
            List<Beverage> topBeverages = rankedBeverages.stream()
                    .limit(topK)
                    .collect(Collectors.toList());
            
            // 转换为VO
            beverageVOs = topBeverages.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
        }
        
        // 6. 处理Wiki内容
        List<WikiContentVO> wikiContentVOs = new ArrayList<>();
        try {
            // 从beverage_vector表查询所有wiki类型的向量
            List<BeverageVector> wikiVectors = beverageVectorMapper.selectList(
                    new LambdaQueryWrapper<BeverageVector>()
                            .eq(BeverageVector::getSourceType, "wiki")
            );
            
            if (!wikiVectors.isEmpty()) {
                // 计算每个wiki向量与查询向量的相似度
                List<WikiContentWithSimilarity> wikiWithSimilarity = new ArrayList<>();
                for (BeverageVector wikiVector : wikiVectors) {
                    if (wikiVector.getVector() == null || wikiVector.getVector().isEmpty()) {
                        continue;
                    }
                    
                    // 解析向量
                    List<Float> vector = embeddingService.jsonToVector(wikiVector.getVector());
                    if (vector.isEmpty() || vector.size() != queryVector.size()) {
                        continue;
                    }
                    
                    // 计算余弦相似度
                    double similarity = calculateCosineSimilarity(queryVector, vector);
                    wikiWithSimilarity.add(new WikiContentWithSimilarity(wikiVector, similarity));
                }
                
                // 按相似度排序，取Top-K
                List<BeverageVector> topWikiVectors = wikiWithSimilarity.stream()
                        .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                        .limit(topK)
                        .map(w -> w.beverageVector)
                        .collect(Collectors.toList());
                
                // 转换为WikiContentVO
                for (BeverageVector wikiVector : topWikiVectors) {
                    WikiContentWithSimilarity wikiWithSim = wikiWithSimilarity.stream()
                            .filter(w -> w.beverageVector.getId().equals(wikiVector.getId()))
                            .findFirst()
                            .orElse(null);
                    
                    WikiContentVO wikiContentVO = convertToWikiContentVO(wikiVector, 
                            wikiWithSim != null ? wikiWithSim.similarity : 0.0);
                    wikiContentVOs.add(wikiContentVO);
                }
            }
        } catch (Exception e) {
            log.warn("获取Wiki内容失败", e);
        }
        
        // 7. 生成推荐理由（结合Beverage和Wiki内容）
        String recommendationReason = null;
        if (request.getIncludeReason() != null && request.getIncludeReason()) {
            // 获取Beverage详细信息
            List<Beverage> topBeverages = new ArrayList<>();
            if (!beverageVOs.isEmpty()) {
                List<Long> beverageIds = beverageVOs.stream()
                        .map(BeverageSummaryVO::getId)
                        .collect(Collectors.toList());
                topBeverages = beverageMapper.selectBatchIds(beverageIds);
            }
            
            // 获取Wiki向量详细信息
            List<BeverageVector> topWikiVectors = new ArrayList<>();
            if (!wikiContentVOs.isEmpty()) {
                List<Long> wikiIds = wikiContentVOs.stream()
                        .map(WikiContentVO::getId)
                        .collect(Collectors.toList());
                // 使用 selectBatchIds 查询（BaseMapper 提供的方法）
                topWikiVectors = beverageVectorMapper.selectBatchIds(wikiIds);
            }
            
            recommendationReason = generateRecommendationReasonWithWiki(
                    request.getQuery(), topBeverages, topWikiVectors, userPreference, request.getConversationHistory());
        }
        
        // 8. 构建响应
        return RAGRecommendationResponse.builder()
                .beverages(beverageVOs)
                .wikiContents(wikiContentVOs)
                .recommendationReason(recommendationReason)
                .queryUnderstanding(queryUnderstanding)
                .build();
    }
    
    /**
     * 查询理解
     */
    private RAGRecommendationResponse.QueryUnderstanding understandQuery(String query) {
        // 简单实现：提取关键词
        List<String> keywords = extractKeywords(query);
        
        // 识别意图和场景
        String intent = identifyIntent(query);
        String scene = identifyScene(query);
        String tastePreference = identifyTastePreference(query);
        
        return RAGRecommendationResponse.QueryUnderstanding.builder()
                .keywords(keywords)
                .intent(intent)
                .scene(scene)
                .tastePreference(tastePreference)
                .build();
    }
    
    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 简单实现：移除停用词，提取关键词
        String[] stopWords = {"的", "了", "和", "是", "就", "都", "而", "及", "与", "或", "我", "你", "他", "她", "它"};
        List<String> keywords = new ArrayList<>();
        
        String[] words = query.split("[\\s，,。.、；;：:！!？?]");
        for (String word : words) {
            word = word.trim();
            if (word.length() > 1 && !Arrays.asList(stopWords).contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }
    
    /**
     * 识别意图
     */
    private String identifyIntent(String query) {
        if (query.contains("推荐") || query.contains("推荐")) {
            return "推荐";
        }
        if (query.contains("适合") || query.contains("适合")) {
            return "适合";
        }
        if (query.contains("想要") || query.contains("想要")) {
            return "需求";
        }
        return "查询";
    }
    
    /**
     * 识别场景
     */
    private String identifyScene(String query) {
        if (query.contains("晚餐") || query.contains("晚餐")) {
            return "晚餐";
        }
        if (query.contains("聚会") || query.contains("聚会")) {
            return "聚会";
        }
        if (query.contains("约会") || query.contains("约会")) {
            return "约会";
        }
        if (query.contains("商务") || query.contains("商务")) {
            return "商务";
        }
        return null;
    }
    
    /**
     * 识别口味偏好
     */
    private String identifyTastePreference(String query) {
        if (query.contains("清淡") || query.contains("清淡")) {
            return "清淡";
        }
        if (query.contains("浓郁") || query.contains("浓郁")) {
            return "浓郁";
        }
        if (query.contains("甜") || query.contains("甜")) {
            return "甜";
        }
        if (query.contains("苦") || query.contains("苦")) {
            return "苦";
        }
        return null;
    }
    
    /**
     * 构建查询向量（融合用户偏好和对话历史）
     */
    private List<Float> buildQueryVector(String query, UserPreferenceVO userPreference, 
                                         List<RAGQueryRequest.ConversationTurn> conversationHistory) {
        // 构建增强查询文本（包含对话历史上下文）
        String enhancedQuery = buildEnhancedQuery(query, conversationHistory);
        
        // 基础查询向量（基于增强查询）
        List<Float> queryVector = embeddingService.embedText(enhancedQuery);
        
        if (userPreference != null && userPreference.getPreferenceVector() != null 
                && !userPreference.getPreferenceVector().isEmpty()) {
            // 融合用户偏好向量（加权平均）
            List<Float> preferenceVector = userPreference.getPreferenceVector();
            
            // 确保维度一致
            int minDim = Math.min(queryVector.size(), preferenceVector.size());
            List<Float> fusedVector = new ArrayList<>();
            
            for (int i = 0; i < minDim; i++) {
                // 查询70% + 偏好30%
                float fused = queryVector.get(i) * 0.7f + preferenceVector.get(i) * 0.3f;
                fusedVector.add(fused);
            }
            
            return fusedVector;
        }
        
        return queryVector;
    }
    
    /**
     * 构建增强查询（包含对话历史上下文）
     */
    private String buildEnhancedQuery(String currentQuery, List<RAGQueryRequest.ConversationTurn> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return currentQuery;
        }
        
        // 构建上下文摘要（只使用最近的几轮对话）
        StringBuilder context = new StringBuilder();
        int historySize = Math.min(conversationHistory.size(), 3); // 最多使用最近3轮对话
        
        for (int i = conversationHistory.size() - historySize; i < conversationHistory.size(); i++) {
            RAGQueryRequest.ConversationTurn turn = conversationHistory.get(i);
            if (turn.getUserQuery() != null && !turn.getUserQuery().trim().isEmpty()) {
                context.append("之前的问题：").append(turn.getUserQuery()).append("；");
            }
        }
        
        // 将当前查询和上下文结合
        if (context.length() > 0) {
            return context.toString() + "当前问题：" + currentQuery;
        }
        
        return currentQuery;
    }
    
    /**
     * 重排序
     */
    private List<Beverage> rerank(List<Beverage> candidates, UserPreferenceVO userPreference, List<Float> queryVector) {
        // 计算每个候选酒类的综合分数
        Map<Long, Double> scores = new HashMap<>();
        
        // 热门度分数
        Map<Long, Double> popularityScores = popularityService.calculatePopularityScores(
                candidates.stream().map(Beverage::getId).collect(Collectors.toList())
        );
        
        for (Beverage beverage : candidates) {
            double score = 0.0;
            
            // 1. 相似度分数（假设已通过向量检索排序，这里给基础分）
            score += 0.4;
            
            // 2. 用户偏好匹配度
            if (userPreference != null) {
                double preferenceScore = calculatePreferenceScore(beverage, userPreference);
                score += preferenceScore * 0.4;
            }
            
            // 3. 热门度分数
            double popularityScore = popularityScores.getOrDefault(beverage.getId(), 0.0);
            score += popularityScore * 0.2;
            
            scores.put(beverage.getId(), score);
        }
        
        // 按分数排序
        return candidates.stream()
                .sorted((a, b) -> Double.compare(
                        scores.getOrDefault(b.getId(), 0.0),
                        scores.getOrDefault(a.getId(), 0.0)
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 计算用户偏好匹配度
     */
    private double calculatePreferenceScore(Beverage beverage, UserPreferenceVO userPreference) {
        double score = 0.0;
        
        // 类型匹配
        if (beverage.getType() != null && userPreference.getTypePreference() != null) {
            score += userPreference.getTypePreference().getOrDefault(beverage.getType(), 0.0) * 0.4;
        }
        
        // 口味匹配
        if (beverage.getTasteNotes() != null && userPreference.getTastePreference() != null) {
            String[] keywords = beverage.getTasteNotes().split("[\\s，,。.、；;：:！!？?]");
            double tasteScore = 0.0;
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (keyword.length() > 1) {
                    tasteScore += userPreference.getTastePreference().getOrDefault(keyword, 0.0);
                }
            }
            score += Math.min(tasteScore / keywords.length, 1.0) * 0.4;
        }
        
        // 产地匹配
        if (beverage.getOrigin() != null && userPreference.getOriginPreference() != null) {
            score += userPreference.getOriginPreference().getOrDefault(beverage.getOrigin(), 0.0) * 0.2;
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 生成推荐理由（已废弃，使用 generateRecommendationReasonWithWiki）
     */
    @Deprecated
    private String generateRecommendationReason(String query, List<Beverage> beverages, UserPreferenceVO userPreference) {
        // 构建上下文（包含酒类信息和相关外部文档知识）
        StringBuilder context = new StringBuilder();
        
        // 添加酒类信息（强调品牌名称）
        for (Beverage beverage : beverages) {
            context.append("品牌：").append(beverage.getName())
                    .append("（").append(beverage.getType()).append("）");
            if (beverage.getNameEn() != null && !beverage.getNameEn().isEmpty()) {
                context.append("，英文名：").append(beverage.getNameEn());
            }
            if (beverage.getOrigin() != null) {
                context.append("，产地：").append(beverage.getOrigin());
            }
            if (beverage.getTasteNotes() != null) {
                context.append("，口感：").append(beverage.getTasteNotes());
            }
            if (beverage.getDescription() != null) {
                context.append("，描述：").append(beverage.getDescription());
            }
            context.append("。\n");
        }
        
        // 添加相关的Wiki知识（从MySQL查询）
        try {
            // 检索Wiki内容（source_type='wiki'）
            List<BeverageVector> wikiDocs = beverageVectorMapper.selectList(
                    new LambdaQueryWrapper<BeverageVector>()
                            .eq(BeverageVector::getSourceType, "wiki")
                            .orderByDesc(BeverageVector::getCreatedAt)
                            .last("limit 5")
            );
            
            // 添加Wiki知识
            if (!wikiDocs.isEmpty()) {
                context.append("\nWiki知识库：\n");
                int count = 0;
                for (BeverageVector doc : wikiDocs) {
                    if (count >= 3) break;
                    if (doc.getTextContent() != null) {
                        String content = doc.getTextContent();
                        // 限制长度
                        if (content.length() > 200) {
                            content = content.substring(0, 200) + "...";
                        }
                        context.append(content).append("\n");
                        count++;
                    }
                }
            }
            
            // 注意：外部文档现在只存储在Milvus中，不存储在MySQL
            // 如果需要外部文档的文本内容，需要从Milvus检索结果中获取
            // 当前Milvus集合结构不包含文本字段，如需文本内容请修改Milvus集合结构
        } catch (Exception e) {
            log.warn("获取Wiki知识失败", e);
        }
        
        // 构建用户偏好描述
        String userPreferenceDesc = "";
        if (userPreference != null) {
            StringBuilder prefDesc = new StringBuilder();
            if (userPreference.getTypePreference() != null && !userPreference.getTypePreference().isEmpty()) {
                String topType = userPreference.getTypePreference().entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("");
                if (!topType.isEmpty()) {
                    prefDesc.append("偏好").append(topType).append("类型");
                }
            }
            userPreferenceDesc = prefDesc.toString();
        }
        
        return llmClient.generateRecommendationReason(query, context.toString(), userPreferenceDesc, "");
    }
    
    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            float v1 = vector1.get(i);
            float v2 = vector2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (norm1 * norm2);
    }
    
    /**
     * 转换为WikiContentVO
     */
    private WikiContentVO convertToWikiContentVO(BeverageVector beverageVector, Double similarity) {
        String textContent = beverageVector.getTextContent();
        String title = extractTitleFromText(textContent);
        String summary = extractSummaryFromText(textContent);
        String content = textContent;
        
        // 从文本内容中提取slug（如果有的话）
        String slug = null;
        if (textContent != null && textContent.contains("词条标题：")) {
            // 尝试从文本中提取更多信息
            String[] parts = textContent.split("词条标题：");
            if (parts.length > 1) {
                String titlePart = parts[1].split("。")[0];
                // 简单的slug生成（实际应该从wiki_page表查询）
                slug = titlePart.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", "-");
            }
        }
        
        return WikiContentVO.builder()
                .id(beverageVector.getId())
                .title(title != null ? title : "Wiki内容")
                .summary(summary)
                .content(content)
                .slug(slug)
                .similarity(similarity)
                .build();
    }
    
    /**
     * 从文本内容中提取标题
     */
    private String extractTitleFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // 查找"词条标题："后面的内容
        if (text.contains("词条标题：")) {
            String[] parts = text.split("词条标题：");
            if (parts.length > 1) {
                String titlePart = parts[1];
                // 提取到第一个"。"之前的内容
                int dotIndex = titlePart.indexOf("。");
                if (dotIndex > 0) {
                    return titlePart.substring(0, dotIndex).trim();
                }
                return titlePart.trim();
            }
        }
        
        // 如果没有找到标题标记，返回前50个字符作为标题
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }
    
    /**
     * 从文本内容中提取摘要
     */
    private String extractSummaryFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // 查找"摘要："后面的内容
        if (text.contains("摘要：")) {
            String[] parts = text.split("摘要：");
            if (parts.length > 1) {
                String summaryPart = parts[1];
                // 提取到第一个"。"之前的内容
                int dotIndex = summaryPart.indexOf("。");
                if (dotIndex > 0) {
                    return summaryPart.substring(0, dotIndex).trim();
                }
                return summaryPart.trim();
            }
        }
        
        // 如果没有找到摘要标记，返回前200个字符作为摘要
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
    
    /**
     * 生成推荐理由（结合Beverage和Wiki内容，考虑对话历史）
     */
    private String generateRecommendationReasonWithWiki(String query, List<Beverage> beverages, 
                                                         List<BeverageVector> wikiVectors, UserPreferenceVO userPreference,
                                                         List<RAGQueryRequest.ConversationTurn> conversationHistory) {
        // 构建上下文（包含酒类信息和Wiki知识）
        StringBuilder context = new StringBuilder();
        
        // 添加酒类信息（强调品牌名称）
        if (!beverages.isEmpty()) {
            context.append("推荐酒类（品牌名称）：\n");
            for (Beverage beverage : beverages) {
                context.append("品牌：").append(beverage.getName())
                        .append("（").append(beverage.getType()).append("）");
                if (beverage.getNameEn() != null && !beverage.getNameEn().isEmpty()) {
                    context.append("，英文名：").append(beverage.getNameEn());
                }
                if (beverage.getOrigin() != null) {
                    context.append("，产地：").append(beverage.getOrigin());
                }
                if (beverage.getTasteNotes() != null) {
                    context.append("，口感：").append(beverage.getTasteNotes());
                }
                if (beverage.getDescription() != null) {
                    context.append("，描述：").append(beverage.getDescription());
                }
                context.append("。\n");
            }
        }
        
        // 添加Wiki知识
        if (!wikiVectors.isEmpty()) {
            context.append("\nWiki知识库：\n");
            int count = 0;
            for (BeverageVector doc : wikiVectors) {
                if (count >= 5) break;
                if (doc.getTextContent() != null) {
                    String content = doc.getTextContent();
                    // 限制长度
                    if (content.length() > 300) {
                        content = content.substring(0, 300) + "...";
                    }
                    context.append(content).append("\n\n");
                    count++;
                }
            }
        }
        
        // 构建用户偏好描述
        String userPreferenceDesc = "";
        if (userPreference != null) {
            StringBuilder prefDesc = new StringBuilder();
            if (userPreference.getTypePreference() != null && !userPreference.getTypePreference().isEmpty()) {
                String topType = userPreference.getTypePreference().entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("");
                if (!topType.isEmpty()) {
                    prefDesc.append("偏好").append(topType).append("类型");
                }
            }
            userPreferenceDesc = prefDesc.toString();
        }
        
        // 构建对话历史上下文
        String conversationContext = buildConversationContext(conversationHistory);
        
        return llmClient.generateRecommendationReason(query, context.toString(), userPreferenceDesc, conversationContext);
    }
    
    /**
     * 从Wiki内容生成推荐理由（已废弃，保留以兼容旧代码）
     */
    @Deprecated
    private String generateRecommendationReasonFromWiki(String query, List<BeverageVector> wikiVectors, UserPreferenceVO userPreference) {
        // 构建上下文（包含Wiki内容）
        StringBuilder context = new StringBuilder();
        
        // 添加Wiki内容
        for (BeverageVector wikiVector : wikiVectors) {
            if (wikiVector.getTextContent() != null) {
                String content = wikiVector.getTextContent();
                // 限制长度
                if (content.length() > 300) {
                    content = content.substring(0, 300) + "...";
                }
                context.append(content).append("\n\n");
            }
        }
        
        // 构建用户偏好描述
        String userPreferenceDesc = "";
        if (userPreference != null) {
            StringBuilder prefDesc = new StringBuilder();
            if (userPreference.getTypePreference() != null && !userPreference.getTypePreference().isEmpty()) {
                String topType = userPreference.getTypePreference().entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("");
                if (!topType.isEmpty()) {
                    prefDesc.append("偏好").append(topType).append("类型");
                }
            }
            userPreferenceDesc = prefDesc.toString();
        }
        
        return llmClient.generateRecommendationReason(query, context.toString(), userPreferenceDesc, "");
    }
    
    /**
     * 构建对话历史上下文
     */
    private String buildConversationContext(List<RAGQueryRequest.ConversationTurn> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("对话历史：\n");
        
        // 只使用最近的几轮对话（最多3轮）
        int startIndex = Math.max(0, conversationHistory.size() - 3);
        for (int i = startIndex; i < conversationHistory.size(); i++) {
            RAGQueryRequest.ConversationTurn turn = conversationHistory.get(i);
            if (turn.getUserQuery() != null && !turn.getUserQuery().trim().isEmpty()) {
                context.append("用户：").append(turn.getUserQuery()).append("\n");
            }
            if (turn.getAiResponse() != null && !turn.getAiResponse().trim().isEmpty()) {
                // 截取AI回答的前100个字符作为摘要
                String response = turn.getAiResponse();
                if (response.length() > 100) {
                    response = response.substring(0, 100) + "...";
                }
                context.append("AI：").append(response).append("\n");
            }
            context.append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * 转换为VO
     */
    private BeverageSummaryVO convertToVO(Beverage beverage) {
        return BeverageSummaryVO.builder()
                .id(beverage.getId())
                .name(beverage.getName())
                .type(beverage.getType())
                .origin(beverage.getOrigin())
                .coverImageId(beverage.getCoverImageId())
                .rating(beverage.getRating())
                .build();
    }
}

