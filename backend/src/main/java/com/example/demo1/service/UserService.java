package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.dto.request.RegisterRequest;
import com.example.demo1.dto.request.UpdateProfileRequest;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.dto.response.UserProfileVO;
import com.example.demo1.entity.User;
import com.example.demo1.entity.UserFollow;
import com.example.demo1.mapper.UserFollowMapper;
import com.example.demo1.mapper.UserMapper;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final PasswordEncoder passwordEncoder;

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
        if (existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在，请换一个新的昵称");
        }
        if (existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAvatarUrl(String.format(DEFAULT_AVATAR_TEMPLATE, request.getUsername()));
        user.setBio("欢迎来到饮品圈，我正在持续探索新的佳酿。");
        userMapper.insert(user);
        return user;
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
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);
        if (request.getAvatarImageId() != null) {
            updateWrapper.set(User::getAvatarImageId, request.getAvatarImageId());
        } else if (StringUtils.isNotBlank(request.getAvatarUrl())) {
            updateWrapper.set(User::getAvatarUrl, request.getAvatarUrl().trim());
        }
        if (request.getBio() != null) {
            updateWrapper.set(User::getBio, request.getBio());
        }
        userMapper.update(null, updateWrapper);
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
        long followerCount = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFolloweeId, target.getId()));
        long followingCount = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, target.getId()));
        boolean isSelf = Objects.equals(currentUserId, target.getId());
        boolean isFollowing = !isSelf && currentUserId != null
                && userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, currentUserId)
                        .eq(UserFollow::getFolloweeId, target.getId())) > 0;

        return UserProfileVO.builder()
                .id(target.getId())
                .username(target.getUsername())
                .email(target.getEmail())
                .role(target.getRole())
                .avatarUrl(buildAvatarUrl(target))
                .bio(target.getBio())
                .createdAt(target.getCreatedAt())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .following(isFollowing)
                .self(isSelf)
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
            return "/files/" + user.getAvatarImageId();
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
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isBlank(keyword)) {
            wrapper.orderByDesc(User::getCreatedAt).last("limit " + limit);
        } else {
            boolean isNumeric = StringUtils.isNumeric(keyword);
            wrapper.and(w -> {
                if (isNumeric) {
                    w.eq(User::getId, Long.parseLong(keyword));
                } else {
                    w.like(User::getUsername, keyword);
                }
            }).last("limit " + limit);
        }
        return userMapper.selectList(wrapper).stream()
                .map(this::buildSimpleUser)
                .collect(Collectors.toList());
    }

    public Map<Long, User> mapByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return findUsersByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }
}
