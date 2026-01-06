package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.enums.WikiStatus;
import com.example.demo1.entity.BeverageVector;
import com.example.demo1.entity.DailyQuestion;
import com.example.demo1.entity.WikiPage;
import com.example.demo1.mapper.BeverageVectorMapper;
import com.example.demo1.mapper.WikiPageMapper;
import com.example.demo1.rag.client.AliyunLLMClient;
import com.example.demo1.rag.service.EmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyQuestionAutoGenerator {

    private final EmbeddingService embeddingService;
    private final BeverageVectorMapper beverageVectorMapper;
    private final WikiPageMapper wikiPageMapper;
    private final AliyunLLMClient llmClient;
    private final ObjectMapper objectMapper;

    public Optional<DailyQuestion> generate(LocalDate date) {
        try {
            Candidate candidate = pickWikiCandidate(date);
            if (candidate == null || StringUtils.isBlank(candidate.context)) {
                return Optional.empty();
            }

            String json = llmClient.generateDailyQuestionJson(candidate.context, candidate.wikiTitle);
            if (StringUtils.isBlank(json)) {
                return Optional.empty();
            }

            Generated generated = parseGenerated(json);
            if (generated == null || !generated.isValid()) {
                log.warn("每日一题解析失败或内容不合法，raw={}", json);
                return Optional.empty();
            }

            DailyQuestion q = new DailyQuestion();
            q.setQuestionDate(date);
            q.setQuestion(safeVarchar(generated.question));
            q.setOptionA(safeVarchar(generated.options.get(0)));
            q.setOptionB(safeVarchar(generated.options.get(1)));
            q.setOptionC(safeVarchar(generated.options.get(2)));
            q.setOptionD(safeVarchar(generated.options.get(3)));
            q.setCorrectOption(generated.correctIndex);
            q.setCountA(0);
            q.setCountB(0);
            q.setCountC(0);
            q.setCountD(0);
            q.setExplanation(StringUtils.trimToNull(generated.explanation));
            q.setWikiLink(candidate.wikiLink);
            return Optional.of(q);
        } catch (Exception e) {
            log.warn("每日一题生成失败", e);
            return Optional.empty();
        }
    }

    private Generated parseGenerated(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Generated generated = new Generated();
            generated.question = StringUtils.trimToNull(asString(map.get("question")));
            generated.explanation = StringUtils.trimToNull(asString(map.get("explanation")));
            generated.correctIndex = asInteger(map.get("correctIndex"));

            Object optionsObj = map.get("options");
            if (optionsObj instanceof List<?>) {
                List<?> list = (List<?>) optionsObj;
                List<String> options = new ArrayList<>();
                for (Object item : list) {
                    String text = StringUtils.trimToNull(asString(item));
                    if (text != null) {
                        options.add(text);
                    }
                }
                generated.options = options;
            }
            return generated;
        } catch (Exception e) {
            return null;
        }
    }

    private Candidate pickWikiCandidate(LocalDate date) {
        String query = pickThemeQuery(date);
        List<BeverageVector> wikiVectors = beverageVectorMapper.selectList(
            new LambdaQueryWrapper<BeverageVector>()
                .eq(BeverageVector::getSourceType, "wiki")
                .select(BeverageVector::getTextContent, BeverageVector::getVector)
                .last("limit 2000")
        );
        if (wikiVectors.isEmpty()) {
            return pickWikiCandidateFromWikiPage(date);
        }

        List<Float> queryVector = embeddingService.embedText(query);
        BeverageVector selected;
        if (queryVector.isEmpty()) {
            int index = Math.floorMod((int) date.toEpochDay(), wikiVectors.size());
            selected = wikiVectors.get(index);
        } else {
            List<Scored> scored = new ArrayList<>();
            for (BeverageVector v : wikiVectors) {
                List<Float> vector = embeddingService.jsonToVector(v.getVector());
                double similarity = cosineSimilarity(queryVector, vector);
                scored.add(new Scored(v, similarity));
            }
            scored.sort(Comparator.comparingDouble((Scored s) -> s.similarity).reversed());
            int topN = Math.min(30, scored.size());
            int index = Math.floorMod((int) date.toEpochDay(), topN);
            selected = scored.get(index).vector;
        }

        String wikiTitle = extractWikiTitle(selected.getTextContent());
        String wikiLink = resolveWikiLink(wikiTitle);
        String context = StringUtils.trimToNull(selected.getTextContent());
        if (context != null && context.length() > 2500) {
            context = context.substring(0, 2500);
        }
        return new Candidate(wikiTitle, wikiLink, context);
    }

    private Candidate pickWikiCandidateFromWikiPage(LocalDate date) {
        List<WikiPage> pages = wikiPageMapper.selectList(
            new LambdaQueryWrapper<WikiPage>()
                .eq(WikiPage::getStatus, WikiStatus.PUBLISHED)
                .select(WikiPage::getTitle, WikiPage::getSummary, WikiPage::getContent, WikiPage::getSlug)
                .last("limit 500")
        );
        if (pages.isEmpty()) {
            return null;
        }
        int index = Math.floorMod((int) date.toEpochDay(), pages.size());
        WikiPage page = pages.get(index);

        StringBuilder text = new StringBuilder();
        if (StringUtils.isNotBlank(page.getTitle())) {
            text.append("词条标题：").append(page.getTitle().trim()).append("。");
        }
        if (StringUtils.isNotBlank(page.getSummary())) {
            text.append("摘要：").append(page.getSummary().trim()).append("。");
        }
        if (StringUtils.isNotBlank(page.getContent())) {
            String content = page.getContent()
                .replaceAll("#+\\s*", "")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("\\*([^*]+)\\*", "$1")
                .replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1")
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`([^`]+)`", "$1");
            text.append("内容：").append(content);
        }

        String wikiLink = StringUtils.isBlank(page.getSlug()) ? null : ("/wiki/" + page.getSlug());
        String context = StringUtils.trimToNull(text.toString());
        if (context != null && context.length() > 2500) {
            context = context.substring(0, 2500);
        }
        return new Candidate(StringUtils.trimToNull(page.getTitle()), wikiLink, context);
    }

    private String resolveWikiLink(String wikiTitle) {
        if (StringUtils.isBlank(wikiTitle)) {
            return null;
        }
        WikiPage page = wikiPageMapper.selectOne(
            new LambdaQueryWrapper<WikiPage>()
                .eq(WikiPage::getStatus, WikiStatus.PUBLISHED)
                .eq(WikiPage::getTitle, wikiTitle)
                .last("limit 1")
        );
        if (page == null || StringUtils.isBlank(page.getSlug())) {
            return null;
        }
        return "/wiki/" + page.getSlug();
    }

    private String extractWikiTitle(String text) {
        if (StringUtils.isBlank(text) || !text.contains("词条标题：")) {
            return null;
        }
        try {
            String[] parts = text.split("词条标题：", 2);
            if (parts.length < 2) {
                return null;
            }
            String rest = parts[1];
            int dotIndex = rest.indexOf("。");
            String title = dotIndex > 0 ? rest.substring(0, dotIndex) : rest;
            return StringUtils.trimToNull(title);
        } catch (Exception e) {
            return null;
        }
    }

    private String pickThemeQuery(LocalDate date) {
        List<String> themes = List.of(
            "葡萄酒 酿造 工艺 关键步骤",
            "威士忌 熟成 橡木桶 风味来源",
            "鸡尾酒 调制 技法 与配比",
            "啤酒 发酵 类型 与风味差异",
            "烈酒 蒸馏 原理 与酒精度",
            "酒吧文化 术语 与礼仪",
            "无酒精饮品 调配 平衡与口感"
        );
        int index = Math.floorMod(date.getDayOfYear(), themes.size());
        return themes.get(index);
    }

    private double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.size() != b.size()) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            float x = a.get(i);
            float y = b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String safeVarchar(String value) {
        String trimmed = StringUtils.trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() > 250 ? trimmed.substring(0, 250) : trimmed;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static final class Scored {
        private final BeverageVector vector;
        private final double similarity;

        private Scored(BeverageVector vector, double similarity) {
            this.vector = vector;
            this.similarity = similarity;
        }
    }

    private static final class Candidate {
        private final String wikiTitle;
        private final String wikiLink;
        private final String context;

        private Candidate(String wikiTitle, String wikiLink, String context) {
            this.wikiTitle = wikiTitle;
            this.wikiLink = wikiLink;
            this.context = context;
        }
    }

    private static final class Generated {
        private String question;
        private List<String> options;
        private Integer correctIndex;
        private String explanation;

        private boolean isValid() {
            if (StringUtils.isBlank(question)) {
                return false;
            }
            if (options == null || options.size() != 4) {
                return false;
            }
            if (correctIndex == null || correctIndex < 0 || correctIndex > 3) {
                return false;
            }
            return options.stream().allMatch(StringUtils::isNotBlank);
        }
    }
}
