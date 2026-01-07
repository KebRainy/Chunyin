package com.example.demo1.service;

import com.example.demo1.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PythonModerationClient {

    private final ObjectMapper objectMapper;

    @Value("${moderation.python.enabled:false}")
    private boolean enabled;

    @Value("${moderation.python.base-url:http://127.0.0.1:8099}")
    private String baseUrl;

    @Value("${moderation.python.timeout-ms:800}")
    private long timeoutMs;

    @Value("${moderation.python.fail-open:true}")
    private boolean failOpen;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofMillis(300))
        .build();

    public Optional<ModerationResponse> moderateText(String text, String scene) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = Map.of(
                "text", text,
                "content", text,
                "scene", scene
            );
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/moderate/text"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Optional.of(objectMapper.readValue(response.body(), ModerationResponse.class));
            }
            throw new BusinessException("本地审核服务返回异常: " + response.statusCode());
        } catch (Exception e) {
            if (failOpen) {
                log.warn("本地审核服务不可用，已按 fail-open 放行: {}", e.getMessage());
                return Optional.empty();
            }
            throw new BusinessException("本地审核服务不可用: " + e.getMessage());
        }
    }

    public Optional<ModerationResponse> moderateImage(byte[] imageBytes, String mimeType, String scene) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            if (imageBytes == null || imageBytes.length == 0) {
                return Optional.empty();
            }
            String b64 = Base64.getEncoder().encodeToString(imageBytes);
            Map<String, Object> payload = Map.of(
                "image_base64", b64,
                "imageBase64", b64,
                "mime_type", mimeType,
                "mimeType", mimeType,
                "scene", scene
            );
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/moderate/image"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Optional.of(objectMapper.readValue(response.body(), ModerationResponse.class));
            }
            throw new BusinessException("本地审核服务返回异常: " + response.statusCode());
        } catch (Exception e) {
            if (failOpen) {
                log.warn("本地审核服务不可用，已按 fail-open 放行: {}", e.getMessage());
                return Optional.empty();
            }
            throw new BusinessException("本地审核服务不可用: " + e.getMessage());
        }
    }

    public record ModerationResponse(
        String action,
        List<String> categories,
        Map<String, Double> scores,
        List<String> reasons
    ) {}
}
