package com.example.demo1.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FileUrlResolver {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public String resolve(String uuid) {
        if (!StringUtils.hasText(uuid)) {
            return null;
        }
        return normalizeContextPath() + "/files/" + uuid;
    }

    private String normalizeContextPath() {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            return "";
        }
        String value = contextPath.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
