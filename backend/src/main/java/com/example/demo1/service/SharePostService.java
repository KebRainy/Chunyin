package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.dto.request.SharePostRequest;
import com.example.demo1.dto.response.SharePostVO;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.entity.SharePost;
import com.example.demo1.entity.User;
import com.example.demo1.mapper.SharePostMapper;
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
    private final UserService userService;

    public SharePostVO createPost(Long userId, SharePostRequest request, String ipAddress) {
        SharePost post = new SharePost();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setLocation(request.getLocation());
        post.setIpAddress(ipAddress);
        post.setImageUrls(request.getImageUrls() == null ? new ArrayList<>() : request.getImageUrls());
        sharePostMapper.insert(post);
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

    public List<SharePostVO> search(String keyword, int limit) {
        LambdaQueryWrapper<SharePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(keyword), SharePost::getContent, keyword)
            .orderByDesc(SharePost::getCreatedAt)
            .last("limit " + limit);
        List<SharePost> posts = sharePostMapper.selectList(wrapper);
        Map<Long, User> userMap = userService.mapByIds(posts.stream().map(SharePost::getUserId).collect(Collectors.toList()));
        return posts.stream().map(post -> toVo(post, userMap.get(post.getUserId()))).collect(Collectors.toList());
    }

    private SharePostVO toVo(SharePost post, User user) {
        SimpleUserVO author = userService.buildSimpleUser(user);
        return SharePostVO.builder()
            .id(post.getId())
            .author(author)
            .content(post.getContent())
            .imageUrls(post.getImageUrls() == null ? List.of() : post.getImageUrls())
            .location(post.getLocation())
            .ipAddressMasked(IpUtils.maskIp(post.getIpAddress()))
            .createdAt(post.getCreatedAt())
            .build();
    }
}
