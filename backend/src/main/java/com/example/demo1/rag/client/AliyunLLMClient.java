package com.example.demo1.rag.client;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.example.demo1.config.RAGConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云通义千问LLM客户端
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AliyunLLMClient {
    
    private final RAGConfig ragConfig;
    private final Generation generation = new Generation();
    private static final int DAILY_QUESTION_MAX_CONTEXT_CHARS = 2500;
    
    /**
     * 生成推荐理由
     */
    public String generateRecommendationReason(String query, String context, String userPreference, String conversationHistory) {
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(buildSystemPrompt())
                .build());
            messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(buildUserPrompt(query, context, userPreference, conversationHistory))
                .build());

            String content = call(messages);
            if (content != null) {
                return content;
            }
            return "根据您的需求，我为您推荐了以下酒类。";
        } catch (Exception e) {
            log.error("调用通义千问API失败", e);
            return "根据您的需求，我为您推荐了以下酒类。";
        }
    }

    /**
     * 生成每日一题（返回严格JSON字符串）
     */
    public String generateDailyQuestionJson(String context, String wikiTitle) {
        try {
            String normalizedContext = context == null ? "" : context.trim();
            if (normalizedContext.length() > DAILY_QUESTION_MAX_CONTEXT_CHARS) {
                normalizedContext = normalizedContext.substring(0, DAILY_QUESTION_MAX_CONTEXT_CHARS);
            }

            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(buildDailyQuestionSystemPrompt())
                .build());
            messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(buildDailyQuestionUserPrompt(normalizedContext, wikiTitle))
                .build());

            return call(messages);
        } catch (Exception e) {
            log.error("调用通义千问API失败（每日一题）", e);
            return null;
        }
    }

    private String call(List<Message> messages) {
        try {
            QwenParam param = QwenParam.builder()
                .apiKey(ragConfig.getAliyun().getAccessKeySecret())
                .model(ragConfig.getAliyun().getLlmModel())
                .messages(messages)
                .temperature(ragConfig.getTemperature().floatValue())
                .resultFormat(QwenParam.ResultFormat.MESSAGE)
                .build();

            Object resultObj = generation.call(param);
            return extractContentFromResult(resultObj);
        } catch (Exception e) {
            log.error("调用通义千问失败", e);
            return null;
        }
    }

    private String extractContentFromResult(Object resultObj) {
        if (resultObj == null) {
            return null;
        }
        try {
            java.lang.reflect.Method getOutputMethod = resultObj.getClass().getMethod("getOutput");
            Object output = getOutputMethod.invoke(resultObj);

            if (output != null) {
                java.lang.reflect.Method getChoicesMethod = output.getClass().getMethod("getChoices");
                List<?> choices = (List<?>) getChoicesMethod.invoke(output);

                if (choices != null && !choices.isEmpty()) {
                    Object firstChoice = choices.get(0);
                    java.lang.reflect.Method getMessageMethod = firstChoice.getClass().getMethod("getMessage");
                    Object message = getMessageMethod.invoke(firstChoice);
                    java.lang.reflect.Method getContentMethod = message.getClass().getMethod("getContent");
                    return (String) getContentMethod.invoke(message);
                }
            }
        } catch (Exception e) {
            log.error("使用反射访问结果失败", e);
        }
        return null;
    }
    
    private String buildSystemPrompt() {
        return "你是一位专业的酒类推荐顾问，擅长根据用户的需求和偏好推荐合适的酒类。" +
                "请用专业、友好、简洁的语言为用户生成个性化的推荐理由。" +
                "重要要求：如果是鸡尾酒，推荐必须有具体名字，如果是非鸡尾酒且用户已提及具体酒种名称，推荐则必须具体到品牌名称，不能只推荐酒类类型。" +
                "推荐理由应该包含：1) 为什么推荐这些具体品牌/名字的酒类 2) 这些品牌/名字酒类的特点和优势 3) 适合的场景或搭配建议。" +
                "如果用户当前询问提供了具体的品牌名称，不必再次提及，但当次回答必须与该品牌相关。";
    }
    
    private String buildUserPrompt(String query, String context, String userPreference, String conversationHistory) {
        StringBuilder prompt = new StringBuilder();
        
        // 添加对话历史（如果有）
        if (conversationHistory != null && !conversationHistory.trim().isEmpty()) {
            prompt.append(conversationHistory).append("\n");
        }
        
        prompt.append("用户当前查询：").append(query).append("\n\n");
        
        if (userPreference != null && !userPreference.isEmpty()) {
            prompt.append("用户偏好：").append(userPreference).append("\n\n");
        }
        
        prompt.append("相关酒类信息：\n").append(context).append("\n\n");
        prompt.append("请根据以上信息（包括对话历史），为用户生成个性化的推荐理由（200字以内）。");
        prompt.append("如果对话历史中有相关信息，请结合历史上下文进行推荐。");
        prompt.append("\n\n重要提示：推荐必须具体到品牌名称，不能只推荐酒类类型。");
        prompt.append("如果上下文中提供了具体的品牌名称，必须在推荐理由中明确提及这些品牌名称。");
        prompt.append("如果上下文中没有提供具体品牌，可以基于酒类类型推荐知名品牌，但必须明确说明品牌名称。");
        
        return prompt.toString();
    }

    private String buildDailyQuestionSystemPrompt() {
        return "你是一位严谨的酒类维基知识出题专家。"
            + "你只输出一段可被JSON解析的内容，不能包含任何额外解释、Markdown或代码块。"
            + "题目必须是单选题，必须能从给定上下文中直接推导出唯一正确答案。"
            + "错误选项要具有迷惑性，但不能与上下文明显矛盾或出现无法验证的信息。";
    }

    private String buildDailyQuestionUserPrompt(String context, String wikiTitle) {
        String titleHint = (wikiTitle == null || wikiTitle.trim().isEmpty()) ? "" : ("主题词条：" + wikiTitle.trim() + "\n");
        return titleHint
            + "请基于以下上下文出一道“每日一题”单选题，并返回严格JSON：\n"
            + "上下文：\n"
            + context
            + "\n\n"
            + "输出JSON格式要求（只能输出JSON，不要输出多余内容）：\n"
            + "{"
            + "\"question\":\"...\","
            + "\"options\":[\"...\",\"...\",\"...\",\"...\"],"
            + "\"correctIndex\":0,"
            + "\"explanation\":\"...\""
            + "}\n"
            + "约束：question不超过80字；每个option不超过60字；options必须正好4个且互不相同；correctIndex只能是0-3整数；explanation用1-2句话解释（不超过120字）。";
    }
}
