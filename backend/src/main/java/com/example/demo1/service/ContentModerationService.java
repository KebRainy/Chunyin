package com.example.demo1.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 内容审核服务
 * SF-12: 评论内容审核与举报
 * 
 * 提供基于停用词库、敏感词库和链接检测规则的内容过滤功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentModerationService {

    private final PythonModerationClient pythonModerationClient;

    /**
     * 敏感词集合（高风险，直接屏蔽）
     */
    private Set<String> sensitiveWords;
    
    /**
     * 停用词集合（低风险，需要人工复核）
     */
    private Set<String> stopWords;
    
    /**
     * 广告词集合
     */
    private Set<String> adWords;
    
    /**
     * URL检测正则
     */
    private Pattern urlPattern;
    
    /**
     * 联系方式检测正则（手机号、微信号等）
     */
    private Pattern contactPattern;

    @PostConstruct
    public void init() {
        initSensitiveWords();
        initStopWords();
        initAdWords();
        initPatterns();
        log.info("内容审核服务初始化完成，敏感词: {}个, 停用词: {}个, 广告词: {}个", 
                sensitiveWords.size(), stopWords.size(), adWords.size());
    }

    /**
     * 初始化敏感词库
     * 实际项目中应从数据库或配置文件加载
     */
    private void initSensitiveWords() {
        sensitiveWords = new HashSet<>(Arrays.asList(
            // 政治敏感词（示例）
            "反动", "颠覆", "分裂",
            // 违法词汇
            "毒品", "赌博", "诈骗", "洗钱",
            // 色情低俗
            "色情", "淫秽", "裸聊",
            // 暴力词汇
            "杀人", "自杀", "恐怖袭击",
            // 其他敏感词
            "代开发票", "办证", "枪支"
        ));
    }

    /**
     * 初始化停用词库（需要人工复核的词汇）
     */
    private void initStopWords() {
        stopWords = new HashSet<>(Arrays.asList(
            // 可能的不当言论
            "傻逼", "操你", "妈的", "草泥马", "卧槽",
            "垃圾", "废物", "白痴", "蠢货",
            // 可能的争议性词汇
            "骗子", "黑心", "坑人", "假货"
        ));
    }

    /**
     * 初始化广告词库
     */
    private void initAdWords() {
        adWords = new HashSet<>(Arrays.asList(
            // 广告推广词汇
            "加微信", "加我微信", "私聊", "代理",
            "招商", "加盟", "免费领取", "限时优惠",
            "点击链接", "扫码", "关注公众号",
            "优惠券", "红包", "返利"
        ));
    }

    /**
     * 初始化正则表达式
     */
    private void initPatterns() {
        // URL检测（http/https链接）
        urlPattern = Pattern.compile(
            "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        // 联系方式检测（手机号、微信号等）
        contactPattern = Pattern.compile(
            "(1[3-9]\\d{9})" +  // 手机号
            "|(微信[号:]?\\s*[\\w\\-]+)" +  // 微信号
            "|(QQ[号:]?\\s*\\d{5,12})" +  // QQ号
            "|(V[信xX][号:]?\\s*[\\w\\-]+)",  // 变体微信
            Pattern.CASE_INSENSITIVE
        );
    }

    /**
     * 审核内容
     * 
     * @param content 待审核内容
     * @return 审核结果
     */
    public ModerationResult moderate(String content) {
        if (content == null || content.trim().isEmpty()) {
            return ModerationResult.approved();
        }

        String normalizedContent = normalizeContent(content);
        List<String> violations = new ArrayList<>();
        ModerationLevel level = ModerationLevel.APPROVED;

        // 1. 检查敏感词（高风险，直接拒绝）
        for (String word : sensitiveWords) {
            if (normalizedContent.contains(word)) {
                violations.add("敏感词: " + word);
                level = ModerationLevel.REJECTED;
            }
        }

        // 2. 检查停用词（中风险，需要人工复核）
        if (level != ModerationLevel.REJECTED) {
            for (String word : stopWords) {
                if (normalizedContent.contains(word)) {
                    violations.add("不当言论: " + word);
                    if (level != ModerationLevel.REJECTED) {
                        level = ModerationLevel.PENDING_REVIEW;
                    }
                }
            }
        }

        // 3. 检查广告词
        for (String word : adWords) {
            if (normalizedContent.contains(word)) {
                violations.add("广告嫌疑: " + word);
                if (level == ModerationLevel.APPROVED) {
                    level = ModerationLevel.PENDING_REVIEW;
                }
            }
        }

        // 4. 检查URL链接
        if (urlPattern.matcher(content).find()) {
            violations.add("包含外部链接");
            if (level == ModerationLevel.APPROVED) {
                level = ModerationLevel.PENDING_REVIEW;
            }
        }

        // 5. 检查联系方式
        if (contactPattern.matcher(content).find()) {
            violations.add("包含联系方式");
            if (level == ModerationLevel.APPROVED) {
                level = ModerationLevel.PENDING_REVIEW;
            }
        }

        // 6. 检查重复字符（刷屏）
        if (hasExcessiveRepetition(content)) {
            violations.add("重复字符过多");
            if (level == ModerationLevel.APPROVED) {
                level = ModerationLevel.PENDING_REVIEW;
            }
        }

        // 7. 可选：本地 Python 模型/规则补充（速度优先，失败可 fail-open）
        if (level != ModerationLevel.REJECTED) {
            Optional<PythonModerationClient.ModerationResponse> python = pythonModerationClient.moderateText(content, "TEXT");
            if (python.isPresent()) {
                level = mergePythonDecision(python.get(), level, violations);
            }
        }

        return new ModerationResult(level, violations);
    }

    public ModerationResult moderateImage(byte[] imageBytes, String mimeType, String scene) {
        Optional<PythonModerationClient.ModerationResponse> python = pythonModerationClient.moderateImage(imageBytes, mimeType, scene);
        if (python.isEmpty()) {
            return ModerationResult.approved();
        }
        List<String> violations = new ArrayList<>();
        ModerationLevel level = mergePythonDecision(python.get(), ModerationLevel.APPROVED, violations);
        return new ModerationResult(level, violations);
    }

    private ModerationLevel mergePythonDecision(PythonModerationClient.ModerationResponse python,
                                               ModerationLevel current,
                                               List<String> violations) {
        if (python == null || python.action() == null) {
            return current;
        }
        String action = python.action().toUpperCase(Locale.ROOT);
        if (python.reasons() != null && !python.reasons().isEmpty()) {
            violations.add("本地审核: " + String.join("; ", python.reasons()));
        } else if (python.categories() != null && !python.categories().isEmpty()) {
            violations.add("本地审核: " + String.join(",", python.categories()));
        }

        if ("BLOCK".equals(action)) {
            return ModerationLevel.REJECTED;
        }
        if ("REVIEW".equals(action) && current == ModerationLevel.APPROVED) {
            return ModerationLevel.PENDING_REVIEW;
        }
        return current;
    }

    /**
     * 快速检查是否包含敏感词（用于实时提示）
     */
    public boolean containsSensitiveWord(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        String normalizedContent = normalizeContent(content);
        for (String word : sensitiveWords) {
            if (normalizedContent.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤敏感词（替换为*号）
     */
    public String filterContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        String filtered = content;
        
        // 替换敏感词
        for (String word : sensitiveWords) {
            if (filtered.toLowerCase().contains(word.toLowerCase())) {
                filtered = filtered.replaceAll("(?i)" + Pattern.quote(word), 
                        "*".repeat(word.length()));
            }
        }
        
        // 替换停用词
        for (String word : stopWords) {
            if (filtered.toLowerCase().contains(word.toLowerCase())) {
                filtered = filtered.replaceAll("(?i)" + Pattern.quote(word), 
                        "*".repeat(word.length()));
            }
        }
        
        return filtered;
    }

    /**
     * 标准化内容（用于检测）
     * 去除空格、转小写、处理变体字符
     */
    private String normalizeContent(String content) {
        return content.toLowerCase()
                .replaceAll("\\s+", "")
                // 处理常见变体
                .replace("0", "o")
                .replace("1", "i")
                .replace("3", "e")
                .replace("4", "a")
                .replace("5", "s")
                .replace("@", "a")
                .replace("$", "s");
    }

    /**
     * 检查是否有过多重复字符
     */
    private boolean hasExcessiveRepetition(String content) {
        if (content.length() < 10) {
            return false;
        }
        
        // 检查连续相同字符
        int maxRepeat = 1;
        int currentRepeat = 1;
        for (int i = 1; i < content.length(); i++) {
            if (content.charAt(i) == content.charAt(i - 1)) {
                currentRepeat++;
                maxRepeat = Math.max(maxRepeat, currentRepeat);
            } else {
                currentRepeat = 1;
            }
        }
        
        return maxRepeat >= 5;
    }

    /**
     * 审核级别
     */
    public enum ModerationLevel {
        /**
         * 审核通过
         */
        APPROVED,
        
        /**
         * 待人工复核
         */
        PENDING_REVIEW,
        
        /**
         * 直接拒绝
         */
        REJECTED
    }

    /**
     * 审核结果
     */
    public static class ModerationResult {
        private final ModerationLevel level;
        private final List<String> violations;

        public ModerationResult(ModerationLevel level, List<String> violations) {
            this.level = level;
            this.violations = violations;
        }

        public static ModerationResult approved() {
            return new ModerationResult(ModerationLevel.APPROVED, Collections.emptyList());
        }

        public ModerationLevel getLevel() {
            return level;
        }

        public List<String> getViolations() {
            return violations;
        }

        public boolean isApproved() {
            return level == ModerationLevel.APPROVED;
        }

        public boolean isRejected() {
            return level == ModerationLevel.REJECTED;
        }

        public boolean needsReview() {
            return level == ModerationLevel.PENDING_REVIEW;
        }

        public String getViolationSummary() {
            if (violations.isEmpty()) {
                return "";
            }
            return String.join("; ", violations);
        }
    }
}

