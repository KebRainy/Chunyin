package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.dto.request.ChangePasswordRequest;
import com.example.demo1.dto.request.RegisterRequest;
import com.example.demo1.dto.request.UpdateMessagePolicyRequest;
import com.example.demo1.dto.request.UpdateProfileRequest;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.dto.response.UserProfileVO;
import com.example.demo1.entity.Image;
import com.example.demo1.entity.User;
import com.example.demo1.entity.UserBlock;
import com.example.demo1.entity.UserFollow;
import com.example.demo1.entity.SharePost;
import com.example.demo1.entity.UserBehavior;
import com.example.demo1.common.enums.TargetType;
import com.example.demo1.common.enums.BehaviorType;
import com.example.demo1.dto.response.RecommendedUserVO;
import com.example.demo1.mapper.UserBlockMapper;
import com.example.demo1.mapper.UserFollowMapper;
import com.example.demo1.mapper.UserMapper;
import com.example.demo1.mapper.ImageMapper;
import com.example.demo1.mapper.SharePostMapper;
import com.example.demo1.mapper.UserBehaviorMapper;
import com.example.demo1.util.FileUrlResolver;
import com.example.demo1.util.IpUtils;
import com.example.demo1.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserBlockMapper userBlockMapper;
    private final PasswordEncoder passwordEncoder;
    private final SharePostMapper sharePostMapper;
    private final ImageMapper imageMapper;
    private final FileUrlResolver fileUrlResolver;
    private final UserBehaviorMapper userBehaviorMapper;

    private static final String DEFAULT_AVATAR_TEMPLATE = "https://api.dicebear.com/7.x/thumbs/svg?seed=%s";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return UserPrincipal.fromUser(user);
    }

    @Transactional
    public User register(RegisterRequest request) {
        log.info("开始注册用户: username={}, email={}", request.getUsername(), request.getEmail());
        
        if (existsByUsername(request.getUsername())) {
            log.warn("用户名已存在: {}", request.getUsername());
            throw new BusinessException("用户名已存在，请换一个新的昵称");
        }
        if (existsByEmail(request.getEmail())) {
            log.warn("邮箱已被注册: {}", request.getEmail());
            throw new BusinessException("邮箱已被注册");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAvatarUrl(String.format(DEFAULT_AVATAR_TEMPLATE, request.getUsername()));
        user.setBio("欢迎来到饮品圈，我正在持续探索新的佳酿。");
        
        // 插入用户
        log.debug("准备插入用户到数据库");
        int result = userMapper.insert(user);
        log.info("插入用户结果: result={}, userId={}", result, user.getId());
        
        if (result <= 0) {
            log.error("用户插入失败: result={}", result);
            throw new BusinessException("用户注册失败，请稍后重试");
        }
        
        // 验证插入是否成功（通过ID判断）
        if (user.getId() == null) {
            log.error("用户插入后未获取到ID");
            throw new BusinessException("用户注册失败，未获取到用户ID");
        }
        
        // 再次验证用户是否真的存在于数据库中
        User savedUser = userMapper.selectById(user.getId());
        if (savedUser == null) {
            log.error("用户插入后查询不到: userId={}", user.getId());
            throw new BusinessException("用户注册失败，数据未正确保存");
        }
        
        log.info("用户注册成功: userId={}, username={}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    public User getRequiredUser(Long userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    public User getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User current = getRequiredUser(userId);
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);
        if (StringUtils.isNotBlank(request.getUsername()) && !request.getUsername().equals(current.getUsername())) {
            if (existsByUsername(request.getUsername())) {
                throw new BusinessException("昵称已被使用");
            }
            updateWrapper.set(User::getUsername, request.getUsername().trim());
        }
        if (StringUtils.isNotBlank(request.getEmail()) && !request.getEmail().equals(current.getEmail())) {
            if (existsByEmail(request.getEmail())) {
                throw new BusinessException("邮箱已被使用");
            }
            updateWrapper.set(User::getEmail, request.getEmail().trim());
        }
        if (request.getAvatarImageId() != null) {
            updateWrapper.set(User::getAvatarImageId, request.getAvatarImageId());
            updateWrapper.set(User::getAvatarUrl, null);
        } else if (StringUtils.isNotBlank(request.getAvatarUrl())) {
            updateWrapper.set(User::getAvatarUrl, request.getAvatarUrl().trim());
        }
        if (request.getBio() != null) {
            updateWrapper.set(User::getBio, request.getBio());
        }
        if (request.getGender() != null) {
            updateWrapper.set(User::getGender, request.getGender());
        }
        if (request.getBirthday() != null) {
            updateWrapper.set(User::getBirthday, request.getBirthday());
        }
        userMapper.update(null, updateWrapper);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getRequiredUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    public boolean existsByUsername(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0;
    }

    public boolean existsByEmail(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)) > 0;
    }

    public UserProfileVO buildProfile(User target, Long currentUserId) {
        return buildProfile(target, currentUserId, null);
    }

    public UserProfileVO buildProfile(User target, Long currentUserId, String ipAddress) {
        long followerCount = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFolloweeId, target.getId()));
        long followingCount = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, target.getId()));
        boolean isSelf = Objects.equals(currentUserId, target.getId());
        boolean isFollowing = !isSelf && currentUserId != null
                && userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, currentUserId)
                        .eq(UserFollow::getFolloweeId, target.getId())) > 0;
        long likeReceived = sharePostMapper.selectList(new LambdaQueryWrapper<SharePost>()
                .eq(SharePost::getUserId, target.getId()))
            .stream()
            .map(SharePost::getLikeCount)
            .filter(Objects::nonNull)
            .mapToLong(Integer::longValue)
            .sum();

        String ipRegion = null;
        if (ipAddress != null && !ipAddress.isEmpty()) {
            ipRegion = IpUtils.resolveRegion(ipAddress);
        }

        return UserProfileVO.builder()
                .id(target.getId())
                .username(target.getUsername())
                .email(target.getEmail())
                .role(target.getRole())
                .avatarUrl(buildAvatarUrl(target))
                .bio(target.getBio())
                .gender(target.getGender())
                .birthday(target.getBirthday())
                .level(target.getLevel())
                .createdAt(target.getCreatedAt())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .following(isFollowing)
                .self(isSelf)
                .likeReceived(likeReceived)
                .ipRegion(ipRegion)
                .messagePolicy(isSelf ? target.getMessagePolicy() : null)
                .build();
    }

    public SimpleUserVO buildSimpleUser(User user) {
        if (user == null) {
            return null;
        }
        String avatarUrl = buildAvatarUrl(user);
        return SimpleUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(avatarUrl)
                .bio(user.getBio())
                .build();
    }

    public String buildAvatarUrl(User user) {
        if (user.getAvatarImageId() != null) {
            Image image = imageMapper.selectById(user.getAvatarImageId());
            if (image != null) {
                return fileUrlResolver.resolve(image.getUuid());
            }
        }
        if (StringUtils.isNotBlank(user.getAvatarUrl())) {
            return user.getAvatarUrl();
        }
        return String.format(DEFAULT_AVATAR_TEMPLATE, user.getUsername());
    }

    @Transactional
    public void follow(Long followerId, Long followeeId) {
        if (Objects.equals(followerId, followeeId)) {
            throw new BusinessException("不能关注自己");
        }
        if (userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId)) > 0) {
            return;
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        userFollowMapper.insert(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        userFollowMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId));
    }

    public List<User> findUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(ids);
    }

    public List<SimpleUserVO> searchUsers(String keyword, int limit) {
        return searchUsers(keyword, 1, limit, "time").getItems();
    }

    /**
     * 搜索用户（支持分页和排序）
     * @param keyword 搜索关键字
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param sortBy 排序方式：time（按时间）或 relevance（按相关度）
     * @return 分页结果
     */
    public PageResult<SimpleUserVO> searchUsers(String keyword, int page, int pageSize, String sortBy) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isBlank(keyword)) {
            wrapper.orderByDesc(User::getCreatedAt);
        } else {
            boolean isNumeric = StringUtils.isNumeric(keyword);
            wrapper.and(w -> {
                if (isNumeric) {
                    // 支持按ID搜索
                    w.eq(User::getId, Long.parseLong(keyword));
                } else {
                    // 搜索用户名（昵称）
                    w.like(User::getUsername, keyword);
                }
            });
            
            // 排序
            if ("relevance".equalsIgnoreCase(sortBy)) {
                // 按相关度排序：精确匹配优先
                // 注意：MyBatis-Plus不支持直接按字符串值排序，这里先按时间排序
                // 实际相关度排序可以在应用层实现
                wrapper.orderByDesc(User::getCreatedAt);
            } else {
                wrapper.orderByDesc(User::getCreatedAt);
            }
        }
        
        Page<User> mpPage = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<SimpleUserVO> result = mpPage.getRecords().stream()
                .map(this::buildSimpleUser)
                .collect(Collectors.toList());
        
        return new PageResult<>(mpPage.getTotal(), page, pageSize, result);
    }

    public List<User> getFollowees(Long followerId) {
        List<UserFollow> follows = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, followerId)
                        .orderByDesc(UserFollow::getCreatedAt));
        return follows.stream()
                .map(f -> getUserById(f.getFolloweeId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Map<Long, User> mapByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return findUsersByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    /**
     * 屏蔽用户
     */
    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (Objects.equals(blockerId, blockedId)) {
            throw new BusinessException("不能屏蔽自己");
        }
        getRequiredUser(blockedId); // 确保被屏蔽用户存在
        
        // 检查是否已屏蔽
        if (userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getBlockerId, blockerId)
                .eq(UserBlock::getBlockedId, blockedId)) > 0) {
            return; // 已屏蔽，直接返回
        }
        
        UserBlock block = new UserBlock();
        block.setBlockerId(blockerId);
        block.setBlockedId(blockedId);
        userBlockMapper.insert(block);
    }

    /**
     * 取消屏蔽用户
     */
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        userBlockMapper.delete(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getBlockerId, blockerId)
                .eq(UserBlock::getBlockedId, blockedId));
    }

    /**
     * 检查是否已屏蔽
     */
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getBlockerId, blockerId)
                .eq(UserBlock::getBlockedId, blockedId)) > 0;
    }

    /**
     * 更新私信接收策略
     */
    @Transactional
    public void updateMessagePolicy(Long userId, UpdateMessagePolicyRequest request) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getMessagePolicy, request.getMessagePolicy());
        userMapper.update(null, updateWrapper);
    }

    /**
     * 获取推荐用户列表
     * 包括：可能同关注的人、常看的贴子的发帖人
     */
    public List<RecommendedUserVO> getRecommendedUsers(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 获取已关注的用户ID列表
        List<Long> followingIds = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId))
                .stream()
                .map(UserFollow::getFolloweeId)
                .collect(Collectors.toList());

        // 获取已屏蔽/取消推荐的用户ID列表（使用UserBlock表）
        List<Long> blockedIds = userBlockMapper.selectList(
                new LambdaQueryWrapper<UserBlock>()
                        .eq(UserBlock::getBlockerId, userId))
                .stream()
                .map(UserBlock::getBlockedId)
                .collect(Collectors.toList());

        // 排除自己、已关注、已屏蔽的用户
        Set<Long> excludeIds = new java.util.HashSet<>();
        excludeIds.add(userId);
        excludeIds.addAll(followingIds);
        excludeIds.addAll(blockedIds);

        Map<Long, RecommendedUserVO> recommendedMap = new java.util.LinkedHashMap<>();

        // 获取当前用户已关注的用户ID集合（用于快速查找）
        Set<Long> followingSet = new java.util.HashSet<>(followingIds);

        // 1. 可能同关注的人：找到关注了相同用户的其他用户
        List<Long> sameFollowUserIds = findUsersWithSameFollows(userId, excludeIds, limit / 2);
        for (Long recommendedUserId : sameFollowUserIds) {
            User user = getUserById(recommendedUserId);
            if (user != null && !excludeIds.contains(recommendedUserId)) {
                RecommendedUserVO vo = RecommendedUserVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .avatarUrl(buildAvatarUrl(user))
                        .bio(user.getBio())
                        .reason("可能同关注的人")
                        .following(followingSet.contains(recommendedUserId))
                        .build();
                recommendedMap.put(recommendedUserId, vo);
                excludeIds.add(recommendedUserId);
            }
        }

        // 2. 常看的贴子的发帖人：从用户浏览记录中提取发帖人
        List<Long> postAuthorIds = findPostAuthorsFromViews(userId, excludeIds, limit - recommendedMap.size());
        for (Long authorId : postAuthorIds) {
            if (!recommendedMap.containsKey(authorId)) {
                User user = getUserById(authorId);
                if (user != null && !excludeIds.contains(authorId)) {
                    RecommendedUserVO vo = RecommendedUserVO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .avatarUrl(buildAvatarUrl(user))
                            .bio(user.getBio())
                            .reason("常看的贴子的发帖人")
                            .following(followingSet.contains(authorId))
                            .build();
                    recommendedMap.put(authorId, vo);
                }
            }
        }

        return new java.util.ArrayList<>(recommendedMap.values());
    }

    /**
     * 找到关注了相同用户的其他用户
     */
    private List<Long> findUsersWithSameFollows(Long userId, Set<Long> excludeIds, int limit) {
        // 获取当前用户关注的所有用户ID
        List<Long> userFollowees = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId))
                .stream()
                .map(UserFollow::getFolloweeId)
                .collect(Collectors.toList());

        if (userFollowees.isEmpty()) {
            return Collections.emptyList();
        }

        // 找到也关注了这些用户的其他用户
        List<UserFollow> sameFollows = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .in(UserFollow::getFolloweeId, userFollowees)
                        .ne(UserFollow::getFollowerId, userId)
                        .notIn(!excludeIds.isEmpty(), UserFollow::getFollowerId, excludeIds));

        // 统计每个用户与当前用户共同关注的次数
        Map<Long, Long> userScoreMap = sameFollows.stream()
                .collect(Collectors.groupingBy(
                        UserFollow::getFollowerId,
                        Collectors.counting()));

        // 按共同关注数排序，取Top-N
        return userScoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 从用户浏览记录中提取发帖人
     */
    private List<Long> findPostAuthorsFromViews(Long userId, Set<Long> excludeIds, int limit) {
        // 获取用户最近浏览的动态（最近30天）
        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .eq(UserBehavior::getTargetType, TargetType.POST)
                        .eq(UserBehavior::getBehaviorType, BehaviorType.VIEW)
                        .ge(UserBehavior::getCreatedAt, thirtyDaysAgo)
                        .orderByDesc(UserBehavior::getCreatedAt)
                        .last("LIMIT 100"));

        if (behaviors.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取这些动态的发帖人
        List<Long> postIds = behaviors.stream()
                .map(UserBehavior::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        List<SharePost> posts = sharePostMapper.selectBatchIds(postIds);
        Map<Long, Long> authorViewCountMap = new java.util.HashMap<>();

        // 统计每个发帖人被浏览的次数
        for (SharePost post : posts) {
            if (post != null && post.getUserId() != null && !excludeIds.contains(post.getUserId())) {
                authorViewCountMap.put(post.getUserId(),
                        authorViewCountMap.getOrDefault(post.getUserId(), 0L) + 1);
            }
        }

        // 按浏览次数排序，取Top-N
        return authorViewCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 取消推荐用户（记录到UserBlock表）
     */
    @Transactional
    public void blockRecommendedUser(Long userId, Long blockedUserId) {
        blockUser(userId, blockedUserId);
    }
}
