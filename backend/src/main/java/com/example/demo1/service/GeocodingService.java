package com.example.demo1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 地理编码服务
 * 用于将地址转换为经纬度坐标
 */
@Slf4j
@Service
public class GeocodingService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 高德地图API Key（可选配置）
     * 如果没有配置，则使用免费的API服务
     */
    @Value("${geocoding.amap.key:}")
    private String amapKey;

    /**
     * 根据地址获取经纬度
     * 
     * @param address 完整地址（包含省市区和详细地址）
     * @return 包含latitude和longitude的Map，如果获取失败返回null
     */
    public Map<String, Double> geocodeAddress(String address) {
        if (!StringUtils.hasText(address)) {
            log.warn("地址为空，无法进行地理编码");
            return null;
        }

        try {
            // 优先使用高德地图API（如果配置了key）
            if (StringUtils.hasText(amapKey)) {
                return geocodeWithAmap(address);
            } else {
                // 使用免费的API服务
                return geocodeWithFreeAPI(address);
            }
        } catch (Exception e) {
            log.error("地理编码失败，地址: {}", address, e);
            return null;
        }
    }

    /**
     * 使用高德地图API进行地理编码
     */
    private Map<String, Double> geocodeWithAmap(String address) throws Exception {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String apiUrl = String.format("https://restapi.amap.com/v3/geocode/geo?key=%s&address=%s", 
                amapKey, encodedAddress);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(response.body());
            
            if ("1".equals(jsonNode.get("status").asText())) {
                JsonNode geocodes = jsonNode.get("geocodes");
                if (geocodes != null && geocodes.isArray() && geocodes.size() > 0) {
                    String location = geocodes.get(0).get("location").asText();
                    String[] coords = location.split(",");
                    if (coords.length == 2) {
                        Map<String, Double> result = new HashMap<>();
                        result.put("longitude", Double.parseDouble(coords[0])); // 经度
                        result.put("latitude", Double.parseDouble(coords[1]));  // 纬度
                        log.info("高德地图地理编码成功，地址: {}, 经纬度: {}", address, location);
                        return result;
                    }
                }
            }
        }
        
        log.warn("高德地图地理编码失败，地址: {}", address);
        return null;
    }

    /**
     * 使用免费API服务进行地理编码（备用方案）
     * 使用Nominatim OpenStreetMap API（免费，但可能有速率限制）
     */
    private Map<String, Double> geocodeWithFreeAPI(String address) throws Exception {
        // 构建查询地址（优先使用中国地址格式）
        String queryAddress = address;
        
        // 使用Nominatim API（OpenStreetMap）
        String encodedAddress = URLEncoder.encode(queryAddress, StandardCharsets.UTF_8);
        String apiUrl = String.format("https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1&addressdetails=1", 
                encodedAddress);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "BeveragePlatform/1.0")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode jsonArray = OBJECT_MAPPER.readTree(response.body());
            
            if (jsonArray.isArray() && jsonArray.size() > 0) {
                JsonNode firstResult = jsonArray.get(0);
                String lat = firstResult.get("lat").asText();
                String lon = firstResult.get("lon").asText();
                
                Map<String, Double> result = new HashMap<>();
                result.put("latitude", Double.parseDouble(lat));
                result.put("longitude", Double.parseDouble(lon));
                log.info("免费API地理编码成功，地址: {}, 经纬度: ({}, {})", address, lat, lon);
                return result;
            }
        }
        
        log.warn("免费API地理编码失败，地址: {}", address);
        return null;
    }

    /**
     * 根据省市区和详细地址构建完整地址并获取经纬度
     * 
     * @param province 省份
     * @param city 城市
     * @param district 区县
     * @param address 详细地址
     * @return 包含latitude和longitude的Map，如果获取失败返回null
     */
    public Map<String, Double> geocodeAddress(String province, String city, String district, String address) {
        StringBuilder fullAddress = new StringBuilder();
        
        if (StringUtils.hasText(province)) {
            fullAddress.append(province);
        }
        if (StringUtils.hasText(city)) {
            fullAddress.append(city);
        }
        if (StringUtils.hasText(district)) {
            fullAddress.append(district);
        }
        if (StringUtils.hasText(address)) {
            fullAddress.append(address);
        }
        
        return geocodeAddress(fullAddress.toString());
    }
}

