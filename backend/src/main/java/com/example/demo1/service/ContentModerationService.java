package com.example.demo1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 内容审核服务（Python-only）
 *
 * 说明：本项目不再保留 Java 端的敏感词/正则“自研规则”审核逻辑，
 * 统一通过本地 Python 服务进行审核（可在 application.properties 中关闭）。
 */
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final PythonModerationClient pythonModerationClient;

    public ModerationResult moderate(String content) {
        if (content == null || content.trim().isEmpty()) {
            return ModerationResult.approved();
        }
        Optional<PythonModerationClient.ModerationResponse> python = pythonModerationClient.moderateText(content, "TEXT");
        if (python.isEmpty()) {
            return ModerationResult.approved();
        }
        return toResult(python.get());
    }

    public ModerationResult moderateImage(byte[] imageBytes, String mimeType, String scene) {
        Optional<PythonModerationClient.ModerationResponse> python = pythonModerationClient.moderateImage(imageBytes, mimeType, scene);
        if (python.isEmpty()) {
            return ModerationResult.approved();
        }
        return toResult(python.get());
    }

    public String filterContent(String content) {
        // REVIEW 阶段目前没有“自动脱敏发布”的可靠方式：上层应按审核结果拦截发布
        return content;
    }

    private ModerationResult toResult(PythonModerationClient.ModerationResponse python) {
        if (python == null || python.action() == null) {
            return ModerationResult.approved();
        }
        String action = python.action().toUpperCase(Locale.ROOT);
        List<String> violations = python.reasons() != null ? python.reasons() : Collections.emptyList();

        if ("BLOCK".equals(action)) {
            return new ModerationResult(ModerationLevel.REJECTED, violations);
        }
        if ("REVIEW".equals(action)) {
            // 当前无人工复核队列：REVIEW 也直接拦截，避免广告/色情漏网
            return new ModerationResult(ModerationLevel.REJECTED, violations);
        }
        return new ModerationResult(ModerationLevel.APPROVED, violations);
    }

    public enum ModerationLevel {
        APPROVED,
        PENDING_REVIEW,
        REJECTED
    }

    public static class ModerationResult {
        private final ModerationLevel level;
        private final List<String> violations;

        public ModerationResult(ModerationLevel level, List<String> violations) {
            this.level = level;
            this.violations = violations == null ? Collections.emptyList() : violations;
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

