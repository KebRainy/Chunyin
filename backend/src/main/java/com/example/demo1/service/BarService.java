package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo1.algorithm.BarRankingAlgorithm;
import com.example.demo1.common.enums.BarSortStrategy;
import com.example.demo1.common.enums.BarStatus;
import com.example.demo1.common.enums.UserRole;
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
import com.example.demo1.entity.Alcohol;
import com.example.demo1.mapper.AlcoholMapper;
import com.example.demo1.mapper.BarApplicationMapper;
import com.example.demo1.mapper.BarMapper;
import com.example.demo1.mapper.BarReviewMapper;
import com.example.demo1.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarService {

    private final BarMapper barMapper;
    private final BarApplicationMapper barApplicationMapper;
    private final BarReviewMapper barReviewMapper;
    private final UserMapper userMapper;
    private final AlcoholMapper alcoholMapper;
    private final ContentModerationService contentModerationService;
    private final GeocodingService geocodingService;

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
     * 搜索附近的酒吧（默认综合排序）
     * 直接使用数据库中存储的经纬度计算距离
     */
    public List<BarVO> searchNearbyBars(Double userLatitude, Double userLongitude, Double radiusKm) {
        return searchNearbyBars(userLatitude, userLongitude, radiusKm, BarSortStrategy.COMPREHENSIVE);
    }
    
    /**
     * 搜索附近的酒吧（支持自定义排序策略）
     * SF-11: 附近最佳酒吧排序
     * 
     * @param userLatitude 用户纬度
     * @param userLongitude 用户经度
     * @param radiusKm 搜索半径（公里）
     * @param sortStrategy 排序策略
     * @return 排序后的酒吧列表
     */
    public List<BarVO> searchNearbyBars(Double userLatitude, Double userLongitude, Double radiusKm, BarSortStrategy sortStrategy) {
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
                .collect(Collectors.toList());
        
        // 根据排序策略进行加权排序
        return sortBarsByStrategy(nearbyBars, sortStrategy, radiusKm);
    }
    
    /**
     * 根据排序策略对酒吧列表进行加权排序
     * 
     * 排序算法说明：
     * 1. 距离分数 = 1 - (distance / maxDistance)，距离越近分数越高
     * 2. 评分分数 = avgRating / 5.0，评分越高分数越高
     * 3. 综合分数 = 距离分数 * 距离权重 + 评分分数 * 评分权重
     * 
     * @param bars 酒吧列表
     * @param strategy 排序策略
     * @param maxDistance 最大距离（用于归一化）
     * @return 排序后的列表
     */
    private List<BarVO> sortBarsByStrategy(List<BarVO> bars, BarSortStrategy strategy, double maxDistance) {
        if (bars.isEmpty()) {
            return bars;
        }
        
        // 计算每个酒吧的综合分数
        for (BarVO bar : bars) {
            double score = calculateBarScore(bar, strategy, maxDistance);
            bar.setScore(score);
        }
        
        // 按综合分数降序排序
        return bars.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());
    }
    
    /**
     * 计算单个酒吧的综合分数
     */
    private double calculateBarScore(BarVO bar, BarSortStrategy strategy, double maxDistance) {
        // 距离分数：距离越近分数越高（0-1之间）
        double distanceScore = 1.0 - (bar.getDistance() / maxDistance);
        distanceScore = Math.max(0, Math.min(1, distanceScore)); // 确保在0-1之间
        
        // 评分分数：评分越高分数越高（0-1之间）
        double ratingScore = (bar.getAvgRating() != null ? bar.getAvgRating() : 0.0) / 5.0;
        ratingScore = Math.max(0, Math.min(1, ratingScore)); // 确保在0-1之间
        
        // 额外加权：评价数量多的酒吧更可信
        int reviewCount = bar.getReviewCount() != null ? bar.getReviewCount() : 0;
        double reviewBonus = Math.min(reviewCount / 100.0, 0.1); // 最多加0.1分
        
        // 计算综合分数
        double score = distanceScore * strategy.getDistanceWeight() 
                     + ratingScore * strategy.getRatingWeight()
                     + reviewBonus;
        
        return score;
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
     * 获取所有酒吧（限制数量）
     */
    public List<BarVO> getAllBars(Integer limit) {
        LambdaQueryWrapper<Bar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bar::getIsActive, true)
                .orderByDesc(Bar::getAvgRating)
                .last("LIMIT " + (limit != null ? limit : 20));

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
     * 获取待审核的酒吧申请列表（管理员功能）
     */
    public List<BarApplicationVO> getPendingApplications() {
        LambdaQueryWrapper<BarApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BarApplication::getStatus, BarStatus.PENDING)
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

            // 根据地址获取经纬度
            try {
                Map<String, Double> coordinates = geocodingService.geocodeAddress(
                        application.getProvince(),
                        application.getCity(),
                        application.getDistrict(),
                        application.getAddress()
                );
                
                if (coordinates != null && coordinates.containsKey("latitude") && coordinates.containsKey("longitude")) {
                    bar.setLatitude(coordinates.get("latitude"));
                    bar.setLongitude(coordinates.get("longitude"));
                    log.info("酒吧审核通过，已获取经纬度: ({}, {})", bar.getLatitude(), bar.getLongitude());
                } else {
                    log.warn("酒吧审核通过，但无法获取经纬度，地址: {} {} {} {}", 
                            application.getProvince(), application.getCity(), 
                            application.getDistrict(), application.getAddress());
                }
            } catch (Exception e) {
                log.error("获取酒吧经纬度失败，地址: {} {} {} {}", 
                        application.getProvince(), application.getCity(), 
                        application.getDistrict(), application.getAddress(), e);
                // 即使获取经纬度失败，也继续创建酒吧记录
            }

            barMapper.insert(bar);
            
            // 检查申请者角色，如果是 USER 则提升为 SELLER
            User applicant = userMapper.selectById(application.getApplicantId());
            if (applicant != null && applicant.getRole() == UserRole.USER) {
                LambdaUpdateWrapper<User> userWrapper = new LambdaUpdateWrapper<>();
                userWrapper.eq(User::getId, application.getApplicantId())
                           .set(User::getRole, UserRole.SELLER);
                userMapper.update(null, userWrapper);
                log.info("申请者 {} 角色已从 USER 提升为 SELLER", application.getApplicantId());
            }
        }
    }

    /**
     * 添加酒吧评价
     * SF-12: 集成内容审核功能
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

        // SF-12: 内容审核
        String content = request.getContent();
        if (content != null && !content.trim().isEmpty()) {
            ContentModerationService.ModerationResult moderationResult = 
                    contentModerationService.moderate(content);
            
            // 如果内容被直接拒绝，抛出异常
            if (moderationResult.isRejected()) {
                throw new BusinessException("评价内容包含违规信息，无法发布");
            }
            
            // 如果需要人工审核，过滤敏感词后发布
            if (moderationResult.needsReview()) {
                content = contentModerationService.filterContent(content);
            }
        }

        // 添加评价
        BarReview review = new BarReview();
        review.setBarId(request.getBarId());
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setContent(content);

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

    /**
     * 获取商家拥有的酒吧列表（用于创建活动时选择）
     * 根据main_beverages包含的标签排序，包含标签的排在前面
     *
     * @param ownerId 商家用户ID
     * @param alcoholIds 酒类标签ID列表（可选，用于排序）
     * @return 商家拥有的酒吧列表
     */
    public List<BarVO> getBarsByOwnerId(Long ownerId, List<Long> alcoholIds) {
        // 查询商家拥有的所有活跃酒吧
        LambdaQueryWrapper<Bar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bar::getOwnerId, ownerId)
                .eq(Bar::getIsActive, true)
                .orderByDesc(Bar::getAvgRating); // 默认按评分排序

        List<Bar> bars = barMapper.selectList(wrapper);
        List<BarVO> barVOs = bars.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 如果提供了alcoholIds，根据main_beverages包含的标签排序
        if (alcoholIds != null && !alcoholIds.isEmpty()) {
            // 获取标签名称
            List<String> alcoholNames = alcoholIds.stream()
                    .map(id -> {
                        Alcohol alcohol = alcoholMapper.selectById(id);
                        return alcohol != null ? alcohol.getName() : null;
                    })
                    .filter(name -> name != null)
                    .collect(Collectors.toList());

            if (!alcoholNames.isEmpty()) {
                // 排序：包含标签的排在前面
                barVOs.sort((a, b) -> {
                    boolean aContains = alcoholNames.stream()
                            .anyMatch(name -> a.getMainBeverages() != null &&
                                    a.getMainBeverages().contains(name));
                    boolean bContains = alcoholNames.stream()
                            .anyMatch(name -> b.getMainBeverages() != null &&
                                    b.getMainBeverages().contains(name));

                    if (aContains && !bContains) {
                        return -1; // a排在前面
                    } else if (!aContains && bContains) {
                        return 1; // b排在前面
                    } else {
                        // 都包含或都不包含，按评分排序
                        return Double.compare(
                                b.getAvgRating() != null ? b.getAvgRating() : 0.0,
                                a.getAvgRating() != null ? a.getAvgRating() : 0.0
                        );
                    }
                });
            }
        }

        return barVOs;
    }

    /**
     * 获取推荐的酒吧列表
     * 使用BarRankingAlgorithm算法进行排序
     *
     * @param userLatitude 用户纬度
     * @param userLongitude 用户经度
     * @param limit 返回数量限制
     * @return 推荐的酒吧列表
     */
    public List<BarVO> getRecommendedBars(Double userLatitude, Double userLongitude, Integer limit) {
        if (userLatitude == null || userLongitude == null) {
            throw new BusinessException("请提供有效的经纬度坐标");
        }

        // 默认搜索半径50公里（用于初筛）
        double radiusKm = 50.0;

        // 计算经纬度范围（Bounding Box）
        double latChange = radiusKm / 111.0;
        double lonChange = Math.abs(radiusKm / (111.0 * Math.cos(Math.toRadians(userLatitude))));

        double minLat = userLatitude - latChange;
        double maxLat = userLatitude + latChange;
        double minLon = userLongitude - lonChange;
        double maxLon = userLongitude + lonChange;

        // 获取范围内的活跃酒吧
        LambdaQueryWrapper<Bar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bar::getIsActive, true)
                .isNotNull(Bar::getLatitude)
                .isNotNull(Bar::getLongitude)
                .between(Bar::getLatitude, minLat, maxLat)
                .between(Bar::getLongitude, minLon, maxLon);

        List<Bar> allBars = barMapper.selectList(wrapper);

        // 转换为VO
        List<BarVO> barVOs = allBars.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 使用BarRankingAlgorithm进行排序
        BarRankingAlgorithm.UserLocation userLocation =
                new BarRankingAlgorithm.UserLocation(userLatitude, userLongitude);

        // 使用默认权重（距离0.6，质量0.4）
        BarRankingAlgorithm.RankingPreferences preferences = null;

        List<BarRankingAlgorithm.BarRecommendationResult> results =
                BarRankingAlgorithm.rankBars(barVOs, userLocation, preferences);

        // 提取BarVO并设置距离和分数
        List<BarVO> recommendedBars = results.stream()
                .map(result -> {
                    BarVO bar = result.getBar();
                    bar.setDistance(result.getDistance());
                    bar.setScore(result.getCompositeScore());
                    return bar;
                })
                .limit(limit != null ? limit : 5)
                .collect(Collectors.toList());

        return recommendedBars;
    }
}
