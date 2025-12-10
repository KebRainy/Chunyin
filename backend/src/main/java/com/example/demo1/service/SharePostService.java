package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.dto.request.SharePostRequest;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.entity.Image;
import com.example.demo1.entity.SharePost;
import com.example.demo1.entity.SharePostImage;
import com.example.demo1.entity.User;
import com.example.demo1.mapper.ImageMapper;
import com.example.demo1.mapper.SharePostMapper;
import com.example.demo1.mapper.SharePostImageMapper;
import com.example.demo1.util.IpUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharePostService {

    private final SharePostMapper sharePostMapper;
    private final SharePostImageMapper sharePostImageMapper;
    private final ImageMapper imageMapper;
    private final UserService userService;

    public SharePostVO createPost(Long userId, SharePostRequest request, String ipAddress) {
        SharePost post = new SharePost();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setLocation(request.getLocation());
        post.setIpAddress(ipAddress);
        sharePostMapper.insert(post);

        // 保存关联的图片
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
        return toVo(post, author);
    }

    public PageResult<SharePostVO> listPosts(int page, int pageSize) {
        Page<SharePost> mpPage = sharePostMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<SharePost>().orderByDesc(SharePost::getCreatedAt));
        List<Long> userIds = mpPage.getRecords().stream()
            .map(SharePost::getUserId)
            .collect(Collectors.toList());
        Map<Long, User> userMap = userService.mapByIds(userIds);
        List<SharePostVO> vos = mpPage.getRecords().stream()
            .map(post -> toVo(post, userMap.get(post.getUserId())))
            .collect(Collectors.toList());
        return new PageResult<>(mpPage.getTotal(), (int) mpPage.getCurrent(), (int) mpPage.getSize(), vos);
    }

    public PageResult<SharePostVO> listFollowingPosts(Long userId, int page, int pageSize) {
        // 获取用户关注的所有用户
        List<User> followees = userService.getFollowees(userId);
        List<Long> followeeIds = followees.stream().map(User::getId).collect(Collectors.toList());

        if (followeeIds.isEmpty()) {
            return new PageResult<>(0L, page, pageSize, new ArrayList<>());
        }

        // 查询这些用户发布的动态
        Page<SharePost> mpPage = sharePostMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<SharePost>()
                .in(SharePost::getUserId, followeeIds)
                .orderByDesc(SharePost::getCreatedAt));

        List<Long> userIds = mpPage.getRecords().stream()
            .map(SharePost::getUserId)
            .collect(Collectors.toList());
        Map<Long, User> userMap = userService.mapByIds(userIds);

        List<SharePostVO> vos = mpPage.getRecords().stream()
            .map(post -> toVo(post, userMap.get(post.getUserId())))
            .collect(Collectors.toList());

        return new PageResult<>(mpPage.getTotal(), (int) mpPage.getCurrent(), (int) mpPage.getSize(), vos);
    }

    public PageResult<SharePostVO> listPostsByCity(String city, int page, int pageSize) {
        Page<SharePost> mpPage = sharePostMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<SharePost>()
                .eq(SharePost::getLocation, city)
                .orderByDesc(SharePost::getCreatedAt));

        List<Long> userIds = mpPage.getRecords().stream()
            .map(SharePost::getUserId)
            .collect(Collectors.toList());
        Map<Long, User> userMap = userService.mapByIds(userIds);

        List<SharePostVO> vos = mpPage.getRecords().stream()
            .map(post -> toVo(post, userMap.get(post.getUserId())))
            .collect(Collectors.toList());

        return new PageResult<>(mpPage.getTotal(), (int) mpPage.getCurrent(), (int) mpPage.getSize(), vos);
    }

    public SharePostVO getPostById(Long postId) {
        SharePost post = sharePostMapper.selectById(postId);
        if (post == null) {
            return null;
        }
        User author = userService.getUserById(post.getUserId());
        return toVo(post, author);
    }

    public List<SharePostVO> search(String keyword, int limit) {
        LambdaQueryWrapper<SharePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(keyword), SharePost::getContent, keyword)
            .orderByDesc(SharePost::getCreatedAt)
            .last("limit " + limit);
        List<SharePost> posts = sharePostMapper.selectList(wrapper);
        Map<Long, User> userMap = userService.mapByIds(posts.stream().map(SharePost::getUserId).collect(Collectors.toList()));
        return posts.stream().map(post -> toVo(post, userMap.get(post.getUserId()))).collect(Collectors.toList());
    }

    public List<Long> getPostImageIds(Long postId) {
        return sharePostImageMapper.selectList(
                new LambdaQueryWrapper<SharePostImage>()
                    .eq(SharePostImage::getSharePostId, postId)
                    .orderByAsc(SharePostImage::getImageOrder))
            .stream()
            .map(SharePostImage::getImageId)
            .collect(Collectors.toList());
    }

    private SharePostVO toVo(SharePost post, User user) {
        SimpleUserVO author = userService.buildSimpleUser(user);
        List<Long> imageIds = getPostImageIds(post.getId());
        List<String> imageUrls = imageIds.stream()
            .map(id -> {
                Image image = imageMapper.selectById(id);
                return image != null ? "/files/" + image.getUuid() : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return SharePostVO.builder()
            .id(post.getId())
            .author(author)
            .content(post.getContent())
            .imageUrls(imageUrls)
            .location(post.getLocation())
            .ipAddressMasked(IpUtils.maskIp(post.getIpAddress()))
            .createdAt(post.getCreatedAt())
            .build();
    }
}
