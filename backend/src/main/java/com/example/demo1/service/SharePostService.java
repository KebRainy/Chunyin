package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.common.enums.CollectionTargetType;
import com.example.demo1.common.enums.TimeDimension;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.dto.request.SharePostCommentRequest;
import com.example.demo1.dto.request.SharePostRequest;
import com.example.demo1.dto.response.SharePostCommentVO;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.entity.Image;
import com.example.demo1.entity.SharePost;
import com.example.demo1.entity.SharePostComment;
import com.example.demo1.entity.SharePostImage;
import com.example.demo1.entity.SharePostLike;
import com.example.demo1.entity.User;
import com.example.demo1.mapper.ImageMapper;
import com.example.demo1.mapper.SharePostCommentMapper;
import com.example.demo1.mapper.SharePostImageMapper;
import com.example.demo1.mapper.SharePostLikeMapper;
import com.example.demo1.mapper.SharePostMapper;
import com.example.demo1.util.FileUrlResolver;
import com.example.demo1.util.IpUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharePostService {

    private final SharePostMapper sharePostMapper;
    private final SharePostImageMapper sharePostImageMapper;
    private final ImageMapper imageMapper;
    private final SharePostLikeMapper sharePostLikeMapper;
    private final SharePostCommentMapper sharePostCommentMapper;
    private final CollectionService collectionService;
    private final UserService userService;
    private final FileUrlResolver fileUrlResolver;

    public SharePostVO createPost(Long userId, SharePostRequest request, String ipAddress) {
        SharePost post = new SharePost();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setLocation(request.getLocation());
        post.setIpAddress(ipAddress);
        post.setIpRegion(IpUtils.resolveRegion(ipAddress));
        post.setTags(normalizeTags(request.getTags()));
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setFavoriteCount(0);
        post.setCommentCount(0);
        sharePostMapper.insert(post);

        if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
            for (int i = 0; i < request.getImageIds().size(); i++) {
                SharePostImage image = new SharePostImage();
                image.setSharePostId(post.getId());
                image.setImageId(request.getImageIds().get(i));
                image.setImageOrder(i);
                sharePostImageMapper.insert(image);
            }
        }

        User author = userService.getUserById(userId);
        return toVo(post, author, Collections.emptySet(), Collections.emptySet());
    }

    public PageResult<SharePostVO> listPosts(int page, int pageSize, Long currentUserId) {
        Page<SharePost> mpPage = sharePostMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<SharePost>().orderByDesc(SharePost::getCreatedAt));
        return buildPageResult(mpPage, currentUserId);
    }

    public PageResult<SharePostVO> listFollowingPosts(Long userId, int page, int pageSize) {
        List<User> followees = userService.getFollowees(userId);
        List<Long> followeeIds = followees.stream().map(User::getId).collect(Collectors.toList());
        if (followeeIds.isEmpty()) {
            return new PageResult<>(0L, page, pageSize, new ArrayList<>());
        }
        Page<SharePost> mpPage = sharePostMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<SharePost>()
                .in(SharePost::getUserId, followeeIds)
                .orderByDesc(SharePost::getCreatedAt));
        return buildPageResult(mpPage, userId);
    }

    public PageResult<SharePostVO> listPostsByCity(String city, int page, int pageSize, Long currentUserId) {
        Page<SharePost> mpPage = sharePostMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<SharePost>()
                .eq(StringUtils.isNotBlank(city), SharePost::getLocation, city)
                .orderByDesc(SharePost::getCreatedAt));
        return buildPageResult(mpPage, currentUserId);
    }

    public PageResult<SharePostVO> listPostsByUser(Long targetUserId, int page, int pageSize, Long currentUserId) {
        Page<SharePost> mpPage = sharePostMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<SharePost>()
                .eq(SharePost::getUserId, targetUserId)
                .orderByDesc(SharePost::getCreatedAt));
        return buildPageResult(mpPage, currentUserId);
    }

    public SharePostVO getPostById(Long postId, Long currentUserId, boolean increaseView) {
        SharePost post = sharePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        if (increaseView) {
            int viewCount = post.getViewCount() == null ? 0 : post.getViewCount();
            post.setViewCount(viewCount + 1);
            sharePostMapper.updateById(post);
        }
        User author = userService.getUserById(post.getUserId());
        Set<Long> likedIds = resolveLikedIds(currentUserId, Collections.singletonList(post.getId()));
        Set<Long> favoritedIds = collectionService.findFavoritedIds(currentUserId,
            Collections.singletonList(post.getId()), CollectionTargetType.POST);
        return toVo(post, author, likedIds, favoritedIds);
    }

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        SharePost post = sharePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        SharePostLike existing = sharePostLikeMapper.selectOne(new LambdaQueryWrapper<SharePostLike>()
            .eq(SharePostLike::getPostId, postId)
            .eq(SharePostLike::getUserId, userId));
        int likeCount = post.getLikeCount() == null ? 0 : post.getLikeCount();
        if (existing != null) {
            sharePostLikeMapper.deleteById(existing.getId());
            post.setLikeCount(Math.max(0, likeCount - 1));
            sharePostMapper.updateById(post);
            return false;
        }
        SharePostLike like = new SharePostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        sharePostLikeMapper.insert(like);
        post.setLikeCount(likeCount + 1);
        sharePostMapper.updateById(post);
        return true;
    }

    @Transactional
    public boolean toggleFavorite(Long postId, Long userId) {
        SharePost post = sharePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        return collectionService.togglePostFavorite(userId, post);
    }

    public List<SharePostCommentVO> listComments(Long postId) {
        List<SharePostComment> comments = sharePostCommentMapper.selectList(
            new LambdaQueryWrapper<SharePostComment>()
                .eq(SharePostComment::getPostId, postId)
                .orderByAsc(SharePostComment::getCreatedAt));
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> userMap = userService.mapByIds(
            comments.stream().map(SharePostComment::getUserId).collect(Collectors.toList()));
        Map<Long, SharePostCommentVO> voMap = new LinkedHashMap<>();
        for (SharePostComment comment : comments) {
            SharePostCommentVO vo = SharePostCommentVO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParentId())
                .author(userService.buildSimpleUser(userMap.get(comment.getUserId())))
                .replies(new ArrayList<>())
                .build();
            voMap.put(comment.getId(), vo);
        }
        List<SharePostCommentVO> roots = new ArrayList<>();
        for (SharePostComment comment : comments) {
            SharePostCommentVO current = voMap.get(comment.getId());
            if (comment.getParentId() == null) {
                roots.add(current);
            } else {
                SharePostCommentVO parent = voMap.get(comment.getParentId());
                if (parent != null) {
                    parent.safeReplies().add(current);
                } else {
                    roots.add(current);
                }
            }
        }
        return roots;
    }

    @Transactional
    public SharePostCommentVO createComment(Long postId, Long userId, SharePostCommentRequest request) {
        SharePost post = sharePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        if (request.getParentId() != null) {
            SharePostComment parent = sharePostCommentMapper.selectById(request.getParentId());
            if (parent == null || !Objects.equals(parent.getPostId(), postId)) {
                throw new BusinessException("回复的评论不存在");
            }
        }
        SharePostComment comment = new SharePostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        sharePostCommentMapper.insert(comment);
        int commentCount = post.getCommentCount() == null ? 0 : post.getCommentCount();
        post.setCommentCount(commentCount + 1);
        sharePostMapper.updateById(post);
        User author = userService.getUserById(userId);
        return SharePostCommentVO.builder()
            .id(comment.getId())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .parentId(comment.getParentId())
            .author(userService.buildSimpleUser(author))
            .replies(new ArrayList<>())
            .build();
    }

    public List<SharePostVO> search(String keyword, int limit) {
        LambdaQueryWrapper<SharePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(keyword), SharePost::getContent, keyword)
            .orderByDesc(SharePost::getCreatedAt)
            .last("limit " + limit);
        List<SharePost> posts = sharePostMapper.selectList(wrapper);
        Map<Long, User> userMap = userService.mapByIds(posts.stream().map(SharePost::getUserId).collect(Collectors.toList()));
        return posts.stream()
            .map(post -> toVo(post, userMap.get(post.getUserId()), Collections.emptySet(), Collections.emptySet()))
            .collect(Collectors.toList());
    }

    /**
     * 获取热门动态排行榜
     * 
     * @param timeDimension 时间维度（DAY/WEEK/MONTH/ALL）
     * @param page 页码
     * @param pageSize 每页大小
     * @param currentUserId 当前用户ID
     * @return 热门动态分页结果
     */
    public PageResult<SharePostVO> getHotPosts(TimeDimension timeDimension, int page, int pageSize, Long currentUserId) {
        // 1. 根据时间维度筛选动态
        LambdaQueryWrapper<SharePost> wrapper = new LambdaQueryWrapper<>();
        
        if (timeDimension != TimeDimension.ALL) {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(timeDimension.getDays());
            wrapper.ge(SharePost::getCreatedAt, cutoffTime);
        }
        
        List<SharePost> posts = sharePostMapper.selectList(wrapper);
        
        if (posts.isEmpty()) {
            return new PageResult<>(0L, page, pageSize, Collections.emptyList());
        }
        
        // 2. 计算每个动态的热度分数
        LocalDateTime now = LocalDateTime.now();
        List<PostWithScore> postsWithScores = posts.stream()
            .map(post -> {
                double hotScore = calculateHotScore(post, now);
                return new PostWithScore(post, hotScore);
            })
            .sorted(Comparator.comparingDouble(PostWithScore::getScore).reversed())
            .collect(Collectors.toList());
        
        // 3. 手动分页
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, postsWithScores.size());
        
        if (start >= postsWithScores.size()) {
            return new PageResult<>((long) postsWithScores.size(), page, pageSize, Collections.emptyList());
        }
        
        List<PostWithScore> pagedPosts = postsWithScores.subList(start, end);
        
        // 4. 转换为VO
        List<SharePost> pagedPostEntities = pagedPosts.stream()
            .map(PostWithScore::getPost)
            .collect(Collectors.toList());
        
        List<Long> userIds = pagedPostEntities.stream().map(SharePost::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userService.mapByIds(userIds);
        
        List<Long> postIds = pagedPostEntities.stream().map(SharePost::getId).collect(Collectors.toList());
        Set<Long> likedIds = resolveLikedIds(currentUserId, postIds);
        Set<Long> favoritedIds = collectionService.findFavoritedIds(currentUserId, postIds, CollectionTargetType.POST);
        
        List<SharePostVO> items = pagedPostEntities.stream()
            .map(post -> toVo(post, userMap.get(post.getUserId()), likedIds, favoritedIds))
            .collect(Collectors.toList());
        
        return new PageResult<>((long) postsWithScores.size(), page, pageSize, items);
    }
    
    /**
     * 计算动态的热度分数
     * 热度分 = (浏览量 × 0.1) + (点赞数 × 2) + (收藏数 × 3) + (评论数 × 1.5)
     * 最终得分 = 热度分 × 时间衰减系数
     * 时间衰减系数 = 1 / (1 + 小时差 / 24)
     */
    private double calculateHotScore(SharePost post, LocalDateTime now) {
        // 基础热度分数
        double viewScore = (post.getViewCount() != null ? post.getViewCount() : 0) * 0.1;
        double likeScore = (post.getLikeCount() != null ? post.getLikeCount() : 0) * 2.0;
        double favoriteScore = (post.getFavoriteCount() != null ? post.getFavoriteCount() : 0) * 3.0;
        double commentScore = (post.getCommentCount() != null ? post.getCommentCount() : 0) * 1.5;
        
        double baseScore = viewScore + likeScore + favoriteScore + commentScore;
        
        // 时间衰减系数
        long hoursSinceCreated = Duration.between(post.getCreatedAt(), now).toHours();
        double timeDecay = 1.0 / (1.0 + hoursSinceCreated / 24.0);
        
        return baseScore * timeDecay;
    }
    
    /**
     * 内部类：动态及其热度分数
     */
    private static class PostWithScore {
        private final SharePost post;
        private final double score;
        
        public PostWithScore(SharePost post, double score) {
            this.post = post;
            this.score = score;
        }
        
        public SharePost getPost() {
            return post;
        }
        
        public double getScore() {
            return score;
        }
    }

    private PageResult<SharePostVO> buildPageResult(Page<SharePost> mpPage, Long currentUserId) {
        List<SharePost> records = mpPage.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(0L, (int) mpPage.getCurrent(), (int) mpPage.getSize(), Collections.emptyList());
        }
        List<Long> userIds = records.stream().map(SharePost::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userService.mapByIds(userIds);
        List<Long> postIds = records.stream().map(SharePost::getId).collect(Collectors.toList());
        Set<Long> likedIds = resolveLikedIds(currentUserId, postIds);
        Set<Long> favoritedIds = collectionService.findFavoritedIds(currentUserId, postIds, CollectionTargetType.POST);
        List<SharePostVO> items = records.stream()
            .map(post -> toVo(post, userMap.get(post.getUserId()), likedIds, favoritedIds))
            .collect(Collectors.toList());
        return new PageResult<>(mpPage.getTotal(), (int) mpPage.getCurrent(), (int) mpPage.getSize(), items);
    }

    private Set<Long> resolveLikedIds(Long userId, List<Long> postIds) {
        if (userId == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        return sharePostLikeMapper.selectList(new LambdaQueryWrapper<SharePostLike>()
                .eq(SharePostLike::getUserId, userId)
                .in(SharePostLike::getPostId, postIds))
            .stream()
            .map(SharePostLike::getPostId)
            .collect(Collectors.toSet());
    }

    private SharePostVO toVo(SharePost post, User user, Set<Long> likedIds, Set<Long> favoritedIds) {
        SimpleUserVO author = userService.buildSimpleUser(user);
        List<Long> imageIds = getPostImageIds(post.getId());
        List<String> imageUrls = imageIds.stream()
            .map(id -> {
                Image image = imageMapper.selectById(id);
                return image != null ? fileUrlResolver.resolve(image.getUuid()) : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        List<String> tags = parseTags(post.getTags());
        return SharePostVO.builder()
            .id(post.getId())
            .author(author)
            .content(post.getContent())
            .imageUrls(imageUrls)
            .location(post.getLocation())
            .tags(tags)
            .ipAddressMasked(IpUtils.maskIp(post.getIpAddress()))
            .ipRegion(post.getIpRegion())
            .viewCount(post.getViewCount())
            .likeCount(post.getLikeCount())
            .favoriteCount(post.getFavoriteCount())
            .commentCount(post.getCommentCount())
            .liked(likedIds != null && likedIds.contains(post.getId()))
            .favorited(favoritedIds != null && favoritedIds.contains(post.getId()))
            .createdAt(post.getCreatedAt())
            .build();
    }

    private List<Long> getPostImageIds(Long postId) {
        return sharePostImageMapper.selectList(
                new LambdaQueryWrapper<SharePostImage>()
                    .eq(SharePostImage::getSharePostId, postId)
                    .orderByAsc(SharePostImage::getImageOrder))
            .stream()
            .map(SharePostImage::getImageId)
            .collect(Collectors.toList());
    }

    private List<String> parseTags(String tags) {
        if (StringUtils.isBlank(tags)) {
            return Collections.emptyList();
        }
        String[] parts = tags.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private String normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .limit(5)
            .collect(Collectors.joining(","));
    }
}
