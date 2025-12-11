package com.example.demo1.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Map;

public final class IpUtils {

    private static final Map<String, String> PROVINCE_MAP = Map.ofEntries(
        Map.entry("10", "局域网"),
        Map.entry("58", "北京市"),
        Map.entry("59", "上海市"),
        Map.entry("60", "广东省"),
        Map.entry("61", "浙江省"),
        Map.entry("101", "江苏省"),
        Map.entry("106", "四川省"),
        Map.entry("111", "湖北省"),
        Map.entry("112", "湖南省"),
        Map.entry("113", "山东省"),
        Map.entry("114", "重庆市"),
        Map.entry("115", "福建省"),
        Map.entry("116", "辽宁省"),
        Map.entry("117", "陕西省"),
        Map.entry("118", "天津市")
    );

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

    public static String resolveRegion(String ip) {
        if (!StringUtils.hasText(ip) || !ip.contains(".")) {
            return "未知地区";
        }
        String prefix = ip.split("\\.")[0];
        return PROVINCE_MAP.getOrDefault(prefix, "未知地区");
    }
}
