package com.example.demo1.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class IpUtils {

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    public static String maskIp(String ip) {
        if (!StringUtils.hasText(ip) || !ip.contains(".")) {
            return ip;
        }
        String[] parts = ip.split("\\.");
        if (parts.length < 4) {
            return ip;
        }
        return parts[0] + "." + parts[1] + ".*.*";
    }
}

