package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo1.common.enums.CollectionTargetType;
import com.example.demo1.dto.response.FootprintVO;
import com.example.demo1.entity.UserFootprint;
import com.example.demo1.mapper.UserFootprintMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FootprintService {

    private final UserFootprintMapper userFootprintMapper;

    @Transactional
    public void record(Long userId,
                       CollectionTargetType targetType,
                       Long targetId,
                       String title,
                       String summary,
                       String coverUrl) {
        UserFootprint existing = userFootprintMapper.selectOne(new LambdaQueryWrapper<UserFootprint>()
            .eq(UserFootprint::getUserId, userId)
            .eq(UserFootprint::getTargetType, targetType)
            .eq(UserFootprint::getTargetId, targetId));
        if (existing != null) {
            userFootprintMapper.update(null, new LambdaUpdateWrapper<UserFootprint>()
                .eq(UserFootprint::getId, existing.getId())
                .set(UserFootprint::getVisitedAt, LocalDateTime.now())
                .set(UserFootprint::getTitle, title)
                .set(UserFootprint::getSummary, summary)
                .set(UserFootprint::getCoverUrl, coverUrl));
        } else {
            UserFootprint footprint = new UserFootprint();
            footprint.setUserId(userId);
            footprint.setTargetType(targetType);
            footprint.setTargetId(targetId);
            footprint.setTitle(title);
            footprint.setSummary(summary);
            footprint.setCoverUrl(coverUrl);
            userFootprintMapper.insert(footprint);
        }
        purgeExpired(userId, 7);
    }

    public List<FootprintVO> listRecent(Long userId, int days, CollectionTargetType type) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<UserFootprint> wrapper = new LambdaQueryWrapper<UserFootprint>()
            .eq(UserFootprint::getUserId, userId)
            .ge(UserFootprint::getVisitedAt, threshold)
            .orderByDesc(UserFootprint::getVisitedAt);
        if (type != null) {
            wrapper.eq(UserFootprint::getTargetType, type);
        }
        return userFootprintMapper.selectList(wrapper).stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }

    private FootprintVO toVo(UserFootprint footprint) {
        return FootprintVO.builder()
            .id(footprint.getId())
            .targetType(footprint.getTargetType())
            .targetId(footprint.getTargetId())
            .title(footprint.getTitle())
            .summary(footprint.getSummary())
            .coverUrl(footprint.getCoverUrl())
            .visitedAt(footprint.getVisitedAt())
            .build();
    }

    private void purgeExpired(Long userId, int days) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        userFootprintMapper.delete(new LambdaQueryWrapper<UserFootprint>()
            .eq(UserFootprint::getUserId, userId)
            .lt(UserFootprint::getVisitedAt, deadline));
    }
}

