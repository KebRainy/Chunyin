package com.example.demo1.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

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

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 匹配省份名称的正则表达式（用于去除省份前缀）
    private static final Pattern PROVINCE_PATTERN = Pattern.compile(
            "^(北京市|天津市|上海市|重庆市|河北省|山西省|辽宁省|吉林省|黑龙江省|江苏省|浙江省|安徽省|福建省|江西省|山东省|河南省|湖北省|湖南省|广东省|海南省|四川省|贵州省|云南省|陕西省|甘肃省|青海省|台湾省|内蒙古自治区|广西壮族自治区|西藏自治区|宁夏回族自治区|新疆维吾尔自治区|香港特别行政区|澳门特别行政区)"
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
            return null;
        }

        // 检查是否是本地IP
        if (ip.startsWith("127.") || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "局域网";
        }

        try {
            // 调用API获取IP属地
            String apiUrl = "https://api.vore.top/api/IPdata?ip=" + ip;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonNode = OBJECT_MAPPER.readTree(response.body());
                
                // 尝试多个字段获取城市信息
                String city = null;
                if (jsonNode.has("ipdata") && jsonNode.get("ipdata").has("info1")) {
                    city = jsonNode.get("ipdata").get("info1").asText();
                } else if (jsonNode.has("adcode") && jsonNode.get("adcode").has("c")) {
                    city = jsonNode.get("adcode").get("c").asText();
                } else if (jsonNode.has("ipdata") && jsonNode.get("ipdata").has("info2")) {
                    city = jsonNode.get("ipdata").get("info2").asText();
                }

                if (StringUtils.hasText(city)) {
                    // 去除空格
                    city = city.replaceAll("\\s+", "");
                    // 只显示到市级，去除省份前缀
                    city = extractCityOnly(city);
                    if (StringUtils.hasText(city)) {
                        return city;
                    }
                }
            }
        } catch (Exception e) {
            // API调用失败，静默处理，使用备用方法
        }

        // 如果API调用失败，使用备用方法（基于IP前缀的简单映射）
        String prefix = ip.split("\\.")[0];
        return PROVINCE_MAP.getOrDefault(prefix, null);
    }

    /**
     * 从完整地址中提取城市名称（去除省份前缀）
     * 例如："北京市北京市" -> "北京市"，"广东省深圳市" -> "深圳市"
     */
    private static String extractCityOnly(String address) {
        if (!StringUtils.hasText(address)) {
            return address;
        }

        // 如果地址以省份开头，去除省份前缀
        java.util.regex.Matcher matcher = PROVINCE_PATTERN.matcher(address);
        if (matcher.find()) {
            String province = matcher.group(1);
            // 如果地址就是省份名称（如"北京市"），直接返回
            if (address.equals(province)) {
                return address;
            }
            // 去除省份前缀
            String city = address.substring(province.length());
            // 如果去除后还有内容，返回城市部分
            if (StringUtils.hasText(city)) {
                return city;
            }
            // 如果去除后为空，说明地址就是省份，返回省份
            return province;
        }

        // 如果没有匹配到省份前缀，直接返回原地址
        return address;
    }
}
