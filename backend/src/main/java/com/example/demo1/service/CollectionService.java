package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.common.enums.CollectionTargetType;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.dto.response.CollectionItemVO;
import com.example.demo1.entity.Image;
import com.example.demo1.entity.SharePost;
import com.example.demo1.entity.SharePostImage;
import com.example.demo1.entity.UserCollection;
import com.example.demo1.entity.WikiPage;
import com.example.demo1.mapper.ImageMapper;
import com.example.demo1.mapper.SharePostImageMapper;
import com.example.demo1.mapper.SharePostMapper;
import com.example.demo1.mapper.UserCollectionMapper;
import com.example.demo1.mapper.WikiPageMapper;
import com.example.demo1.util.FileUrlResolver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final UserCollectionMapper userCollectionMapper;
    private final SharePostMapper sharePostMapper;
    private final WikiPageMapper wikiPageMapper;
    private final SharePostImageMapper sharePostImageMapper;
    private final ImageMapper imageMapper;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public boolean togglePostFavorite(Long userId, SharePost post) {
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        UserCollection existing = userCollectionMapper.selectOne(new LambdaQueryWrapper<UserCollection>()
            .eq(UserCollection::getUserId, userId)
            .eq(UserCollection::getTargetType, CollectionTargetType.POST)
            .eq(UserCollection::getTargetId, post.getId()));
        int favoriteCount = post.getFavoriteCount() == null ? 0 : post.getFavoriteCount();
        if (existing != null) {
            userCollectionMapper.deleteById(existing.getId());
            post.setFavoriteCount(Math.max(0, favoriteCount - 1));
            sharePostMapper.updateById(post);
            return false;
        }
        UserCollection collection = new UserCollection();
        collection.setUserId(userId);
        collection.setTargetType(CollectionTargetType.POST);
        collection.setTargetId(post.getId());
        userCollectionMapper.insert(collection);
        post.setFavoriteCount(favoriteCount + 1);
        sharePostMapper.updateById(post);
        return true;
    }

    @Transactional
    public boolean toggleWikiFavorite(Long userId, WikiPage page) {
        if (page == null) {
            throw new BusinessException(404, "词条不存在");
        }
        UserCollection existing = userCollectionMapper.selectOne(new LambdaQueryWrapper<UserCollection>()
            .eq(UserCollection::getUserId, userId)
            .eq(UserCollection::getTargetType, CollectionTargetType.WIKI)
            .eq(UserCollection::getTargetId, page.getId()));
        if (existing != null) {
            userCollectionMapper.deleteById(existing.getId());
            return false;
        }
        UserCollection collection = new UserCollection();
        collection.setUserId(userId);
        collection.setTargetType(CollectionTargetType.WIKI);
        collection.setTargetId(page.getId());
        userCollectionMapper.insert(collection);
        return true;
    }

    public boolean isPostFavorited(Long userId, Long postId) {
        if (userId == null) {
            return false;
        }
        return userCollectionMapper.selectCount(new LambdaQueryWrapper<UserCollection>()
            .eq(UserCollection::getUserId, userId)
            .eq(UserCollection::getTargetType, CollectionTargetType.POST)
            .eq(UserCollection::getTargetId, postId)) > 0;
    }

    public boolean isWikiFavorited(Long userId, Long wikiId) {
        if (userId == null) {
            return false;
        }
        return userCollectionMapper.selectCount(new LambdaQueryWrapper<UserCollection>()
            .eq(UserCollection::getUserId, userId)
            .eq(UserCollection::getTargetType, CollectionTargetType.WIKI)
            .eq(UserCollection::getTargetId, wikiId)) > 0;
    }

    public PageResult<CollectionItemVO> listCollections(Long userId,
                                                        CollectionTargetType targetType,
                                                        int page,
                                                        int pageSize,
                                                        String keyword) {
        LambdaQueryWrapper<UserCollection> wrapper = new LambdaQueryWrapper<UserCollection>()
            .eq(UserCollection::getUserId, userId)
            .orderByDesc(UserCollection::getCreatedAt);
        if (targetType != null) {
            wrapper.eq(UserCollection::getTargetType, targetType);
        }
        Page<UserCollection> mpPage = userCollectionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        if (mpPage.getRecords().isEmpty()) {
            return new PageResult<>(0L, page, pageSize, Collections.emptyList());
        }
        List<Long> postIds = mpPage.getRecords().stream()
            .filter(item -> item.getTargetType() == CollectionTargetType.POST)
            .map(UserCollection::getTargetId)
            .collect(Collectors.toList());
        List<Long> wikiIds = mpPage.getRecords().stream()
            .filter(item -> item.getTargetType() == CollectionTargetType.WIKI)
            .map(UserCollection::getTargetId)
            .collect(Collectors.toList());
        Map<Long, SharePost> postMap = postIds.isEmpty() ? Collections.emptyMap()
            : sharePostMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(SharePost::getId, post -> post));
        Map<Long, String> postCoverMap = resolvePostCover(postIds);
        Map<Long, WikiPage> wikiMap = wikiIds.isEmpty() ? Collections.emptyMap()
            : wikiPageMapper.selectBatchIds(wikiIds).stream()
                .collect(Collectors.toMap(WikiPage::getId, pageEntity -> pageEntity));
        List<CollectionItemVO> items = new ArrayList<>();
        for (UserCollection collection : mpPage.getRecords()) {
            if (collection.getTargetType() == CollectionTargetType.POST) {
                SharePost post = postMap.get(collection.getTargetId());
                if (post == null) {
                    continue;
                }
                items.add(CollectionItemVO.builder()
                    .id(collection.getId())
                    .targetId(post.getId())
                    .targetType(CollectionTargetType.POST)
                    .slug(null)
                    .title(StringUtils.abbreviate(post.getContent(), 50))
                    .summary(StringUtils.abbreviate(post.getContent(), 120))
                    .coverUrl(postCoverMap.get(post.getId()))
                    .createdAt(collection.getCreatedAt())
                    .build());
            } else {
                WikiPage pageEntity = wikiMap.get(collection.getTargetId());
                if (pageEntity == null) {
                    continue;
                }
                String summary = StringUtils.defaultIfBlank(pageEntity.getSummary(), StringUtils.abbreviate(pageEntity.getContent(), 120));
                items.add(CollectionItemVO.builder()
                    .id(collection.getId())
                    .targetId(pageEntity.getId())
                    .targetType(CollectionTargetType.WIKI)
                    .slug(pageEntity.getSlug())
                    .title(pageEntity.getTitle())
                    .summary(summary)
                    .coverUrl(null)
                    .createdAt(collection.getCreatedAt())
                    .build());
            }
        }
        if (StringUtils.isNotBlank(keyword)) {
            String lower = keyword.toLowerCase();
            items = items.stream()
                .filter(item -> (item.getTitle() != null && item.getTitle().toLowerCase().contains(lower))
                    || (item.getSummary() != null && item.getSummary().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
        }
        return new PageResult<>(mpPage.getTotal(), (int) mpPage.getCurrent(), (int) mpPage.getSize(), items);
    }

    public java.util.Set<Long> findFavoritedIds(Long userId, List<Long> targetIds, CollectionTargetType type) {
        if (userId == null || targetIds == null || targetIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return userCollectionMapper.selectList(new LambdaQueryWrapper<UserCollection>()
                .eq(UserCollection::getUserId, userId)
                .eq(UserCollection::getTargetType, type)
                .in(UserCollection::getTargetId, targetIds))
            .stream()
            .map(UserCollection::getTargetId)
            .collect(Collectors.toSet());
    }

    private Map<Long, String> resolvePostCover(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SharePostImage> relations = sharePostImageMapper.selectList(new LambdaQueryWrapper<SharePostImage>()
            .in(SharePostImage::getSharePostId, postIds)
            .orderByAsc(SharePostImage::getImageOrder));
        Map<Long, Long> firstImageIds = new LinkedHashMap<>();
        for (SharePostImage relation : relations) {
            firstImageIds.putIfAbsent(relation.getSharePostId(), relation.getImageId());
        }
        if (firstImageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Image> imageMap = imageMapper.selectBatchIds(firstImageIds.values()).stream()
            .collect(Collectors.toMap(Image::getId, img -> img));
        Map<Long, String> coverMap = new HashMap<>();
        for (Map.Entry<Long, Long> entry : firstImageIds.entrySet()) {
            Image image = imageMap.get(entry.getValue());
            if (image != null) {
                coverMap.put(entry.getKey(), fileUrlResolver.resolve(image.getUuid()));
            }
        }
        return coverMap;
    }
}
