package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.enums.TagCategory;
import com.example.demo1.common.enums.TagSource;
import com.example.demo1.entity.PostTag;
import com.example.demo1.mapper.PostTagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 标签提取服务
 * 自动从内容中提取标签
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagExtractionService {

    private final PostTagMapper postTagMapper;

    // 话题标签正则：#话题名#
    private static final Pattern TOPIC_PATTERN = Pattern.compile("#([^#\\s]{1,20})#");
    
    // 饮品相关关键词
    private static final Set<String> BEVERAGE_KEYWORDS = Set.of(
        "威士忌", "白兰地", "伏特加", "朗姆酒", "金酒", "龙舌兰", "鸡尾酒",
        "啤酒", "葡萄酒", "红酒", "白酒", "清酒", "烧酒", "梅酒",
        "whisky", "whiskey", "brandy", "vodka", "rum", "gin", "tequila",
        "cocktail", "beer", "wine", "sake"
    );
    
    // 地点相关关键词
    private static final Set<String> LOCATION_KEYWORDS = Set.of(
        "酒吧", "bar", "pub", "lounge", "餐厅", "餐吧", "夜店", "club"
    );
    
    // 氛围相关关键词
    private static final Set<String> ATMOSPHERE_KEYWORDS = Set.of(
        "浪漫", "安静", "热闹", "优雅", "放松", "激情", "温馨", "私密",
        "romantic", "quiet", "lively", "elegant", "relaxed", "cozy"
    );

    /**
     * 从内容中提取并保存标签
     * 
     * @param postId 帖子ID
     * @param content 帖子内容
     * @param userTags 用户手动添加的标签
     */
    @Transactional
    public void extractAndSaveTags(Long postId, String content, List<String> userTags) {
        Set<PostTag> tags = new HashSet<>();
        
        // 1. 提取用户手动添加的标签
        if (userTags != null && !userTags.isEmpty()) {
            for (String tag : userTags) {
                if (tag != null && !tag.trim().isEmpty()) {
                    PostTag postTag = createTag(postId, tag.trim(), TagSource.USER, TagCategory.OTHER);
                    tags.add(postTag);
                }
            }
        }
        
        // 2. 提取话题标签 (#话题#)
        if (content != null) {
            Matcher matcher = TOPIC_PATTERN.matcher(content);
            while (matcher.find()) {
                String topic = matcher.group(1);
                PostTag postTag = createTag(postId, topic, TagSource.AUTO, TagCategory.TOPIC);
                tags.add(postTag);
            }
            
            // 3. 自动提取关键词标签
            String lowerContent = content.toLowerCase();
            
            // 饮品标签
            for (String keyword : BEVERAGE_KEYWORDS) {
                if (lowerContent.contains(keyword.toLowerCase())) {
                    PostTag postTag = createTag(postId, keyword, TagSource.AUTO, TagCategory.BEVERAGE);
                    tags.add(postTag);
                }
            }
            
            // 地点标签
            for (String keyword : LOCATION_KEYWORDS) {
                if (lowerContent.contains(keyword.toLowerCase())) {
                    PostTag postTag = createTag(postId, keyword, TagSource.AUTO, TagCategory.LOCATION);
                    tags.add(postTag);
                }
            }
            
            // 氛围标签
            for (String keyword : ATMOSPHERE_KEYWORDS) {
                if (lowerContent.contains(keyword.toLowerCase())) {
                    PostTag postTag = createTag(postId, keyword, TagSource.AUTO, TagCategory.ATMOSPHERE);
                    tags.add(postTag);
                }
            }
        }
        
        // 保存标签（限制最多10个）
        tags.stream()
            .limit(10)
            .forEach(tag -> {
                try {
                    postTagMapper.insert(tag);
                } catch (Exception e) {
                    log.warn("保存标签失败: {}", tag.getTagName(), e);
                }
            });
        
        log.debug("为帖子 {} 提取并保存了 {} 个标签", postId, tags.size());
    }

    /**
     * 删除帖子的所有标签
     */
    @Transactional
    public void deletePostTags(Long postId) {
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>()
            .eq(PostTag::getPostId, postId));
    }

    /**
     * 获取帖子的标签列表
     */
    public List<PostTag> getPostTags(Long postId) {
        return postTagMapper.selectList(new LambdaQueryWrapper<PostTag>()
            .eq(PostTag::getPostId, postId));
    }

    /**
     * 获取热门标签
     */
    public List<Map<String, Object>> getHotTags(int limit) {
        List<PostTag> allTags = postTagMapper.selectList(null);
        
        // 统计标签出现次数
        Map<String, Long> tagCounts = allTags.stream()
            .collect(Collectors.groupingBy(
                PostTag::getTagName,
                Collectors.counting()
            ));
        
        // 按出现次数排序并返回
        return tagCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("tag", entry.getKey());
                map.put("count", entry.getValue());
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * 按标签搜索帖子
     */
    public List<Long> searchPostIdsByTag(String tag) {
        return postTagMapper.selectList(new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getTagName, tag))
            .stream()
            .map(PostTag::getPostId)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 创建标签实体
     */
    private PostTag createTag(Long postId, String tagName, TagSource source, TagCategory category) {
        PostTag tag = new PostTag();
        tag.setPostId(postId);
        tag.setTagName(tagName);
        tag.setSource(source);
        // 将代码中的分类映射到数据库支持的分类
        tag.setTagCategory(mapToDatabaseCategory(category));
        return tag;
    }
    
    /**
     * 将代码中的分类映射到数据库支持的分类
     * 数据库只支持: BEVERAGE_TYPE, TASTE, SCENE, LOCATION, OTHER
     */
    private TagCategory mapToDatabaseCategory(TagCategory category) {
        switch (category) {
            case BEVERAGE:
                return TagCategory.BEVERAGE_TYPE;
            case ATMOSPHERE:
                return TagCategory.SCENE;
            case TOPIC:
                return TagCategory.OTHER;
            default:
                return category;
        }
    }
}

