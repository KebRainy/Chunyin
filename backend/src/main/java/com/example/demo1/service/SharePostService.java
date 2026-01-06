package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.common.enums.BehaviorType;
import com.example.demo1.common.enums.CollectionTargetType;
import com.example.demo1.common.enums.TargetType;
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
    private final FileStorageService fileStorageService;
    private final TagExtractionService tagExtractionService;
    private final UserBehaviorService userBehaviorService;
    private final ContentModerationService contentModerationService;

    @Transactional
    public SharePostVO createPost(Long userId, SharePostRequest request, String ipAddress) {
        SharePost post = new SharePost();
        post.setUserId(userId);

        // 内容审核（文本）
        String content = request.getContent();
        ContentModerationService.ModerationResult moderationResult =
            contentModerationService.moderate(content);
        if (moderationResult.isRejected()) {
            throw new BusinessException("动态内容包含违规信息，无法发布");
        }
        if (moderationResult.needsReview()) {
            content = contentModerationService.filterContent(content);
        }
        post.setContent(content);

        post.setLocation(request.getLocation());
        post.setIpAddress(ipAddress);
        post.setIpRegion(IpUtils.resolveRegion(ipAddress));
        post.setTags(normalizeTags(request.getTags()));
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setFavoriteCount(0);
        post.setCommentCount(0);

        if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
            // 内容审核（图片）：目前无人工复核队列，因此对 REVIEW/BLOCK 均直接拦截发布
            for (Long imageId : request.getImageIds()) {
                Image image = imageMapper.selectById(imageId);
                if (image == null) {
                    throw new BusinessException(400, "图片不存在: " + imageId);
                }
                byte[] data = fileStorageService.loadData(image);
                ContentModerationService.ModerationResult imageModeration =
                    contentModerationService.moderateImage(data, image.getMimeType(), "POST_IMAGE");
                if (!imageModeration.isApproved()) {
                    throw new BusinessException(400, "图片疑似包含违规内容，无法发布");
                }
            }
        }

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

        // 自动提取标签并保存
        tagExtractionService.extractAndSaveTags(post.getId(), content, request.getTags());

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
            // 记录浏览行为
            userBehaviorService.recordBehavior(currentUserId, TargetType.POST, postId, BehaviorType.VIEW);
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
        // 记录点赞行为
        userBehaviorService.recordBehavior(userId, TargetType.POST, postId, BehaviorType.LIKE);
        return true;
    }

    @Transactional
    public boolean toggleFavorite(Long postId, Long userId) {
        SharePost post = sharePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        boolean favorited = collectionService.togglePostFavorite(userId, post);
        // 记录收藏行为（仅在收藏时记录）
        if (favorited) {
            userBehaviorService.recordBehavior(userId, TargetType.POST, postId, BehaviorType.FAVORITE);
        }
        return favorited;
    }

    public List<SharePostCommentVO> listComments(Long postId) {
        List<SharePostComment> comments = sharePostCommentMapper.selectList(
            new LambdaQueryWrapper<SharePostComment>()
                .eq(SharePostComment::getPostId, postId)
                .orderByAsc(SharePostComment::getCreatedAt));
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, SharePostComment> commentMap = comments.stream()
            .collect(Collectors.toMap(SharePostComment::getId, c -> c, (a, b) -> a, LinkedHashMap::new));

        Set<Long> userIds = comments.stream()
            .map(SharePostComment::getUserId)
            .collect(Collectors.toSet());
        Map<Long, User> userMap = userService.mapByIds(new ArrayList<>(userIds));

        Map<Long, SharePostCommentVO> voMap = new LinkedHashMap<>();

        // 仅输出两层评论：
        // - 一级：parentId 为 null
        // - 二级：统一挂在一级下（parentId=一级ID），并且仅当“回复二级评论”时才带 @（replyToUser）
        for (SharePostComment comment : comments) {
            SharePostComment parent = comment.getParentId() != null ? commentMap.get(comment.getParentId()) : null;

            Long outputParentId = null;
            SimpleUserVO replyToUser = null;
            if (parent != null) {
                // 深度为 1：回复一级评论，不展示 @
                if (parent.getParentId() == null) {
                    outputParentId = parent.getId();
                } else {
                    // 深度 >= 2：回复二级及以下评论，输出为二级评论（挂在一级下），并展示 @ 被回复的用户
                    SharePostComment root = parent;
                    while (root.getParentId() != null) {
                        SharePostComment next = commentMap.get(root.getParentId());
                        if (next == null || Objects.equals(next.getId(), root.getId())) {
                            break;
                        }
                        root = next;
                    }
                    outputParentId = root.getId();
                    User parentAuthor = userMap.get(parent.getUserId());
                    if (parentAuthor != null) {
                        replyToUser = userService.buildSimpleUser(parentAuthor);
                    }
                }
            }

            SharePostCommentVO vo = SharePostCommentVO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .parentId(outputParentId)
                .author(userService.buildSimpleUser(userMap.get(comment.getUserId())))
                .replyToUser(replyToUser)
                .replies(new ArrayList<>())
                .build();
            voMap.put(comment.getId(), vo);
        }

        List<SharePostCommentVO> roots = new ArrayList<>();
        for (SharePostCommentVO vo : voMap.values()) {
            if (vo.getParentId() == null) {
                roots.add(vo);
                continue;
            }
            SharePostCommentVO root = voMap.get(vo.getParentId());
            if (root != null) {
                root.safeReplies().add(vo);
            } else {
                // 异常数据：找不到一级评论，则作为一级评论返回
                vo.setParentId(null);
                vo.setReplyToUser(null);
                roots.add(vo);
            }
        }

        return roots;
    }

    /**
     * 创建评论
     * SF-12: 集成内容审核功能
     */
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
        
        // SF-12: 内容审核
        String content = request.getContent();
        ContentModerationService.ModerationResult moderationResult = 
                contentModerationService.moderate(content);
        
        // 如果内容被直接拒绝，抛出异常
        if (moderationResult.isRejected()) {
            throw new BusinessException("评论内容包含违规信息，无法发布");
        }
        
        // 如果需要人工审核，过滤敏感词后发布
        if (moderationResult.needsReview()) {
            content = contentModerationService.filterContent(content);
        }
        
        SharePostComment comment = new SharePostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(content);
        comment.setLikeCount(0);
        sharePostCommentMapper.insert(comment);
        int commentCount = post.getCommentCount() == null ? 0 : post.getCommentCount();
        post.setCommentCount(commentCount + 1);
        sharePostMapper.updateById(post);
        // 记录评论行为
        userBehaviorService.recordBehavior(userId, TargetType.POST, postId, BehaviorType.COMMENT);
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
        return search(keyword, 1, limit, "time").getItems();
    }

    /**
     * 搜索动态（支持分页和排序）
     * @param keyword 搜索关键字
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param sortBy 排序方式：time（按时间）或 relevance（按相关度）
     * @return 分页结果
     */
    public PageResult<SharePostVO> search(String keyword, int page, int pageSize, String sortBy) {
        LambdaQueryWrapper<SharePost> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(keyword)) {
            // 搜索内容或标签
            wrapper.and(w -> {
                w.like(SharePost::getContent, keyword)
                    .or()
                    .like(SharePost::getTags, keyword);
            });
        }
        
        // 排序
        if ("relevance".equalsIgnoreCase(sortBy) && StringUtils.isNotBlank(keyword)) {
            // 按相关度排序：使用MySQL的FIELD函数，内容匹配优先于标签匹配
            // 注意：MyBatis-Plus不支持直接按字符串值排序，这里先按时间排序
            // 实际相关度排序可以在应用层实现
            wrapper.orderByDesc(SharePost::getCreatedAt);
        } else {
            // 按时间排序
            wrapper.orderByDesc(SharePost::getCreatedAt);
        }
        
        Page<SharePost> mpPage = sharePostMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<SharePost> posts = mpPage.getRecords();
        
        if (posts.isEmpty()) {
            return new PageResult<>(mpPage.getTotal(), page, pageSize, Collections.emptyList());
        }
        
        Map<Long, User> userMap = userService.mapByIds(posts.stream().map(SharePost::getUserId).collect(Collectors.toList()));
        List<SharePostVO> result = posts.stream()
            .map(post -> toVo(post, userMap.get(post.getUserId()), Collections.emptySet(), Collections.emptySet()))
            .collect(Collectors.toList());
        
        return new PageResult<>(mpPage.getTotal(), page, pageSize, result);
    }

    /**
     * 删除动态
     * 只有动态作者或管理员可以删除
     */
    @Transactional
    public void deletePost(Long postId, Long userId, boolean isAdmin) {
        SharePost post = sharePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        
        // 检查权限：只有作者或管理员可以删除
        if (!post.getUserId().equals(userId) && !isAdmin) {
            throw new BusinessException(403, "无权删除该动态");
        }
        
        // 删除关联的图片记录
        sharePostImageMapper.delete(new LambdaQueryWrapper<SharePostImage>()
            .eq(SharePostImage::getSharePostId, postId));
        
        // 删除关联的点赞记录
        sharePostLikeMapper.delete(new LambdaQueryWrapper<SharePostLike>()
            .eq(SharePostLike::getPostId, postId));
        
        // 删除关联的评论
        sharePostCommentMapper.delete(new LambdaQueryWrapper<SharePostComment>()
            .eq(SharePostComment::getPostId, postId));
        
        // 删除关联的标签
        tagExtractionService.deletePostTags(postId);
        
        // 删除动态本身
        sharePostMapper.deleteById(postId);
    }

    /**
     * 删除评论
     * 只有评论作者、动态作者或管理员可以删除
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId, boolean isAdmin) {
        SharePostComment comment = sharePostCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        
        // 获取动态信息，检查是否为动态作者
        SharePost post = sharePostMapper.selectById(comment.getPostId());
        boolean isPostAuthor = post != null && post.getUserId().equals(userId);
        boolean isCommentAuthor = comment.getUserId().equals(userId);
        
        // 检查权限
        if (!isCommentAuthor && !isPostAuthor && !isAdmin) {
            throw new BusinessException(403, "无权删除该评论");
        }
        
        // 先删除子评论（回复）
        sharePostCommentMapper.delete(new LambdaQueryWrapper<SharePostComment>()
            .eq(SharePostComment::getParentId, commentId));
        
        // 删除评论本身
        sharePostCommentMapper.deleteById(commentId);
        
        // 更新动态评论数
        if (post != null) {
            int commentCount = post.getCommentCount() == null ? 0 : post.getCommentCount();
            post.setCommentCount(Math.max(0, commentCount - 1));
            sharePostMapper.updateById(post);
        }
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
