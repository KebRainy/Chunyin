package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo1.common.enums.BarStatus;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.dto.request.BarRegisterRequest;
import com.example.demo1.dto.request.BarReviewRequest;
import com.example.demo1.dto.response.BarApplicationVO;
import com.example.demo1.dto.response.BarReviewVO;
import com.example.demo1.dto.response.BarVO;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.entity.Bar;
import com.example.demo1.entity.BarApplication;
import com.example.demo1.entity.BarReview;
import com.example.demo1.entity.User;
import com.example.demo1.mapper.BarApplicationMapper;
import com.example.demo1.mapper.BarMapper;
import com.example.demo1.mapper.BarReviewMapper;
import com.example.demo1.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BarService {

    private final BarMapper barMapper;
    private final BarApplicationMapper barApplicationMapper;
    private final BarReviewMapper barReviewMapper;
    private final UserMapper userMapper;

    /**
     * 注册酒吧（提交申请）
     */
    @Transactional
    public Long registerBar(BarRegisterRequest request, Long userId) {
        BarApplication application = new BarApplication();
        application.setName(request.getName());
        application.setAddress(request.getAddress());
        application.setProvince(request.getProvince());
        application.setCity(request.getCity());
        application.setDistrict(request.getDistrict());
        application.setOpeningTime(request.getOpeningTime());
        application.setClosingTime(request.getClosingTime());
        application.setContactPhone(request.getContactPhone());
        application.setDescription(request.getDescription());
        application.setMainBeverages(request.getMainBeverages());
        application.setApplicantId(userId);
        application.setStatus(BarStatus.PENDING);

        barApplicationMapper.insert(application);
        return application.getId();
    }

    /**
     * 获取酒吧详情
     */
    public BarVO getBarById(Long barId) {
        Bar bar = barMapper.selectById(barId);
        if (bar == null || !bar.getIsActive()) {
            throw new BusinessException("酒吧不存在");
        }
        return convertToVO(bar);
    }

    /**
     * 搜索附近的酒吧
     * 直接使用数据库中存储的经纬度计算距离
     */
    public List<BarVO> searchNearbyBars(Double userLatitude, Double userLongitude, Double radiusKm) {
        // 获取所有活跃且有经纬度信息的酒吧
        LambdaQueryWrapper<Bar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bar::getIsActive, true)
                .isNotNull(Bar::getLatitude)
                .isNotNull(Bar::getLongitude);
        List<Bar> allBars = barMapper.selectList(wrapper);

        // 计算距离并筛选
        List<BarVO> nearbyBars = allBars.stream()
                .map(bar -> {
                    BarVO vo = convertToVO(bar);
                    
                    // 使用Haversine公式计算距离
                    double distance = calculateDistance(
                        userLatitude, userLongitude,
                        bar.getLatitude(), bar.getLongitude()
                    );
                    
                    vo.setDistance(distance);
                    return vo;
                })
                .filter(vo -> vo.getDistance() <= radiusKm)
                .sorted((a, b) -> Double.compare(a.getDistance(), b.getDistance()))
                .collect(Collectors.toList());
        
        return nearbyBars;
    }
    
    /**
     * 使用Haversine公式计算两点之间的距离（单位：公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * 按城市搜索酒吧（支持模糊匹配）
     */
    public List<BarVO> searchBarsByCity(String city) {
        LambdaQueryWrapper<Bar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bar::getIsActive, true)
                .like(Bar::getCity, city)
                .orderByDesc(Bar::getAvgRating);

        List<Bar> bars = barMapper.selectList(wrapper);
        return bars.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 搜索酒吧（综合模糊搜索：名称、城市、地址、主营酒类）
     */
    public List<BarVO> searchBarsByName(String keyword) {
        LambdaQueryWrapper<Bar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bar::getIsActive, true)
                .and(w -> w
                    .like(Bar::getName, keyword)
                    .or().like(Bar::getCity, keyword)
                    .or().like(Bar::getAddress, keyword)
                    .or().like(Bar::getMainBeverages, keyword)
                )
                .orderByDesc(Bar::getAvgRating);

        List<Bar> bars = barMapper.selectList(wrapper);
        return bars.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 获取用户提交的酒吧申请列表
     */
    public List<BarApplicationVO> getApplicationsByApplicant(Long userId) {
        LambdaQueryWrapper<BarApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BarApplication::getApplicantId, userId)
                .eq(BarApplication::getIsActive, true)
                .orderByDesc(BarApplication::getCreatedAt);

        List<BarApplication> applications = barApplicationMapper.selectList(wrapper);
        return applications.stream().map(this::convertToApplicationVO).collect(Collectors.toList());
    }

    /**
     * 审核酒吧申请（管理员功能）
     */
    @Transactional
    public void reviewBarApplication(Long applicationId, BarStatus status, String reviewNote, Long reviewerId) {
        BarApplication application = barApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("酒吧申请不存在");
        }

        // 更新申请状态
        LambdaUpdateWrapper<BarApplication> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BarApplication::getId, applicationId)
                .set(BarApplication::getStatus, status)
                .set(BarApplication::getReviewNote, reviewNote)
                .set(BarApplication::getReviewedBy, reviewerId)
                .set(BarApplication::getReviewedAt, LocalDateTime.now());

        barApplicationMapper.update(null, wrapper);

        // 如果审核通过，创建正式的酒吧记录
        if (status == BarStatus.APPROVED) {
            Bar bar = new Bar();
            bar.setApplicationId(applicationId);
            bar.setName(application.getName());
            bar.setAddress(application.getAddress());
            bar.setProvince(application.getProvince());
            bar.setCity(application.getCity());
            bar.setDistrict(application.getDistrict());
            bar.setOpeningTime(application.getOpeningTime());
            bar.setClosingTime(application.getClosingTime());
            bar.setContactPhone(application.getContactPhone());
            bar.setDescription(application.getDescription());
            bar.setMainBeverages(application.getMainBeverages());
            bar.setOwnerId(application.getApplicantId());

            barMapper.insert(bar);
        }
    }

    /**
     * 添加酒吧评价
     */
    @Transactional
    public Long addBarReview(BarReviewRequest request, Long userId) {
        // 检查酒吧是否存在
        Bar bar = barMapper.selectById(request.getBarId());
        if (bar == null || !bar.getIsActive()) {
            throw new BusinessException("酒吧不存在");
        }

        // 检查用户是否已经评价过
        LambdaQueryWrapper<BarReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BarReview::getBarId, request.getBarId())
                .eq(BarReview::getUserId, userId)
                .eq(BarReview::getIsActive, true);

        BarReview existingReview = barReviewMapper.selectOne(wrapper);
        if (existingReview != null) {
            throw new BusinessException("您已经评价过该酒吧");
        }

        // 添加评价
        BarReview review = new BarReview();
        review.setBarId(request.getBarId());
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setContent(request.getContent());

        barReviewMapper.insert(review);

        // 更新酒吧的平均评分和评价数量
        updateBarRating(request.getBarId());

        return review.getId();
    }

    /**
     * 获取酒吧的评价列表
     */
    public List<BarReviewVO> getBarReviews(Long barId) {
        LambdaQueryWrapper<BarReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BarReview::getBarId, barId)
                .eq(BarReview::getIsActive, true)
                .orderByDesc(BarReview::getCreatedAt);

        List<BarReview> reviews = barReviewMapper.selectList(wrapper);

        // 如果没有评价，直接返回空列表
        if (reviews.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // 获取用户信息
        List<Long> userIds = reviews.stream()
                .map(BarReview::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return reviews.stream().map(review -> {
            User user = userMap.get(review.getUserId());
            return BarReviewVO.builder()
                    .id(review.getId())
                    .barId(review.getBarId())
                    .userId(review.getUserId())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .createdAt(review.getCreatedAt())
                    .user(user != null ? SimpleUserVO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .avatarUrl(user.getAvatarUrl())
                            .bio(user.getBio())
                            .build() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 删除评价
     */
    @Transactional
    public void deleteBarReview(Long reviewId, Long userId) {
        BarReview review = barReviewMapper.selectById(reviewId);
        if (review == null || !review.getIsActive()) {
            throw new BusinessException("评价不存在");
        }

        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该评价");
        }

        LambdaUpdateWrapper<BarReview> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BarReview::getId, reviewId)
                .set(BarReview::getIsActive, false);

        barReviewMapper.update(null, wrapper);

        // 更新酒吧的平均评分和评价数量
        updateBarRating(review.getBarId());
    }

    /**
     * 更新酒吧的平均评分和评价数量
     */
    private void updateBarRating(Long barId) {
        LambdaQueryWrapper<BarReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BarReview::getBarId, barId)
                .eq(BarReview::getIsActive, true);

        List<BarReview> reviews = barReviewMapper.selectList(wrapper);
        
        int reviewCount = reviews.size();
        double avgRating = 0.0;
        
        if (reviewCount > 0) {
            avgRating = reviews.stream()
                    .mapToInt(BarReview::getRating)
                    .average()
                    .orElse(0.0);
        }

        LambdaUpdateWrapper<Bar> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Bar::getId, barId)
                .set(Bar::getAvgRating, avgRating)
                .set(Bar::getReviewCount, reviewCount);

        barMapper.update(null, updateWrapper);
    }

    private BarVO convertToVO(Bar bar) {
        return BarVO.builder()
                .id(bar.getId())
                .name(bar.getName())
                .address(bar.getAddress())
                .province(bar.getProvince())
                .city(bar.getCity())
                .district(bar.getDistrict())
                .latitude(bar.getLatitude())
                .longitude(bar.getLongitude())
                .openingTime(bar.getOpeningTime())
                .closingTime(bar.getClosingTime())
                .contactPhone(bar.getContactPhone())
                .description(bar.getDescription())
                .mainBeverages(bar.getMainBeverages())
                .ownerId(bar.getOwnerId())
                .avgRating(bar.getAvgRating())
                .reviewCount(bar.getReviewCount())
                .createdAt(bar.getCreatedAt())
                .build();
    }

    private BarApplicationVO convertToApplicationVO(BarApplication application) {
        return BarApplicationVO.builder()
                .id(application.getId())
                .name(application.getName())
                .address(application.getAddress())
                .province(application.getProvince())
                .city(application.getCity())
                .district(application.getDistrict())
                .openingTime(application.getOpeningTime())
                .closingTime(application.getClosingTime())
                .contactPhone(application.getContactPhone())
                .description(application.getDescription())
                .mainBeverages(application.getMainBeverages())
                .applicantId(application.getApplicantId())
                .status(application.getStatus())
                .reviewNote(application.getReviewNote())
                .createdAt(application.getCreatedAt())
                .build();
    }
}

