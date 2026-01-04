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
    
    /**
     * 生成推荐理由
     */
    public String generateRecommendationReason(String query, String context, String userPreference, String conversationHistory) {
        try {
            List<Message> messages = new ArrayList<>();
            
            // 系统提示词
            String systemPrompt = buildSystemPrompt();
            messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemPrompt)
                    .build());
            
            // 用户查询和上下文
            String userContent = buildUserPrompt(query, context, userPreference, conversationHistory);
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(userContent)
                    .build());
            
            QwenParam param = QwenParam.builder()
                    .apiKey(ragConfig.getAliyun().getAccessKeySecret())
                    .model(ragConfig.getAliyun().getLlmModel())
                    .messages(messages)
                    .temperature(ragConfig.getTemperature().floatValue())
                    .resultFormat(QwenParam.ResultFormat.MESSAGE)
                    .build();
            
            Object resultObj = generation.call(param);
            
            // 使用反射访问结果
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
                        String content = (String) getContentMethod.invoke(message);
                        return content;
                    }
                }
            } catch (Exception e) {
                log.error("使用反射访问结果失败", e);
            }
            
            return "根据您的需求，我为您推荐了以下酒类。";
        } catch (Exception e) {
            log.error("调用通义千问API失败", e);
            return "根据您的需求，我为您推荐了以下酒类。";
        }
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
}
