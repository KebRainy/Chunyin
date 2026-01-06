package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.algorithm.BarRankingAlgorithm;
import com.example.demo1.common.enums.ActivityStatus;
import com.example.demo1.common.enums.ReviewStatus;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.dto.request.ActivityBarReviewRequest;
import com.example.demo1.dto.request.CreateActivityRequest;
import com.example.demo1.dto.request.ReviewActivityRequest;
import com.example.demo1.dto.response.ActivityVO;
import com.example.demo1.dto.response.BarVO;
import com.example.demo1.dto.response.SimpleUserVO;
import com.example.demo1.entity.Activity;
import com.example.demo1.entity.ActivityParticipant;
import com.example.demo1.entity.Alcohol;
import com.example.demo1.entity.Bar;
import com.example.demo1.entity.BarReview;
import com.example.demo1.entity.Beverage;
import com.example.demo1.entity.User;
import com.example.demo1.mapper.ActivityMapper;
import com.example.demo1.mapper.ActivityParticipantMapper;
import com.example.demo1.mapper.AlcoholMapper;
import com.example.demo1.mapper.BarMapper;
import com.example.demo1.mapper.BarReviewMapper;
import com.example.demo1.mapper.BeverageMapper;
import com.example.demo1.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityParticipantMapper activityParticipantMapper;
    private final BarMapper barMapper;
    private final BarReviewMapper barReviewMapper;
    private final BeverageMapper beverageMapper;
    private final AlcoholMapper alcoholMapper;
    private final UserMapper userMapper;

    /**
     * 创建活动
     */
    @Transactional
    public Long createActivity(CreateActivityRequest request, Long userId) {
        // 验证酒类标签是否存在
        if (request.getAlcoholIds() == null || request.getAlcoholIds().isEmpty()) {
            throw new BusinessException("请至少选择一个酒类标签");
        }
        if (request.getAlcoholIds().size() > 2) {
            throw new BusinessException("最多只能选择2个酒类标签");
        }

        for (Long alcoholId : request.getAlcoholIds()) {
            Alcohol alcohol = alcoholMapper.selectById(alcoholId);
            if (alcohol == null) {
                throw new BusinessException("酒类标签不存在: " + alcoholId);
            }
        }

        // 如果指定了酒吧，验证酒吧是否存在
        if (request.getBarId() != null) {
            Bar bar = barMapper.selectById(request.getBarId());
            if (bar == null || !bar.getIsActive()) {
                throw new BusinessException("酒吧不存在");
            }
        }

        Activity activity = new Activity();
        activity.setOrganizerId(userId);
        activity.setActivityTime(request.getActivityTime());
        // 保留beverageId用于兼容（使用第一个alcoholId对应的beverage，如果没有则设为null）
        activity.setBeverageId(null); // 不再使用beverageId
        activity.setAlcoholIds(request.getAlcoholIds());
        activity.setBarId(request.getBarId());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setRemark(request.getRemark());
        activity.setReviewStatus(ReviewStatus.PENDING);
        activity.setStatus(ActivityStatus.PENDING);

        activityMapper.insert(activity);

        // 自动参与（发起者自动参与）
        ActivityParticipant participant = new ActivityParticipant();
        participant.setActivityId(activity.getId());
        participant.setUserId(userId);
        activityParticipantMapper.insert(participant);

        log.info("User {} created activity {}, alcoholIds: {}", userId, activity.getId(), request.getAlcoholIds());
        return activity.getId();
    }

    /**
     * 根据酒类标签推荐酒吧
     * 根据alcohol标签匹配bar的main_beverages字段
     * 考虑距离、评分等因素
     */
    public List<BarVO> recommendBarsByAlcoholIds(List<Long> alcoholIds, Double userLatitude, Double userLongitude, Integer limit) {
        if (alcoholIds == null || alcoholIds.isEmpty()) {
            throw new BusinessException("请至少选择一个酒类标签");
        }

        // 获取选中的酒类标签名称
        List<String> alcoholNames = alcoholIds.stream()
                .map(id -> {
                    Alcohol alcohol = alcoholMapper.selectById(id);
                    return alcohol != null ? alcohol.getName() : null;
                })
                .filter(name -> name != null)
                .collect(Collectors.toList());

        if (alcoholNames.isEmpty()) {
            throw new BusinessException("无效的酒类标签");
        }

        // 获取所有活跃的酒吧
        LambdaQueryWrapper<Bar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bar::getIsActive, true);

        // 根据main_beverages字段匹配酒类标签
        // main_beverages格式可能是: "威士忌" 或 "威士忌、啤酒" 等
        // 使用OR条件匹配任意一个选中的酒类标签
        if (alcoholNames.size() == 1) {
            wrapper.like(Bar::getMainBeverages, alcoholNames.get(0));
        } else {
            wrapper.and(w -> {
                for (int i = 0; i < alcoholNames.size(); i++) {
                    if (i == 0) {
                        w.like(Bar::getMainBeverages, alcoholNames.get(i));
                    } else {
                        w.or().like(Bar::getMainBeverages, alcoholNames.get(i));
                    }
                }
            });
        }

        List<Bar> matchedBars = barMapper.selectList(wrapper);

        // 转换为VO
        List<BarVO> barVOs = matchedBars.stream()
                .map(this::convertBarToVO)
                .collect(Collectors.toList());

        // 如果用户提供了位置信息，优先推荐附近的酒吧
        if (userLatitude != null && userLongitude != null) {
            // 使用BarRankingAlgorithm进行排序
            BarRankingAlgorithm.UserLocation userLocation = 
                    new BarRankingAlgorithm.UserLocation(userLatitude, userLongitude);
            
            List<BarRankingAlgorithm.BarRecommendationResult> results = 
                    BarRankingAlgorithm.rankBars(barVOs, userLocation, null);

            // 提取BarVO并设置距离和分数
            return results.stream()
                    .map(result -> {
                        BarVO bar = result.getBar();
                        bar.setDistance(result.getDistance());
                        bar.setScore(result.getCompositeScore());
                        return bar;
                    })
                    .limit(limit != null ? limit : 10)
                    .collect(Collectors.toList());
        } else {
            // 如果没有位置信息，按评分排序
            return barVOs.stream()
                    .sorted((a, b) -> {
                        int ratingCompare = b.getAvgRating().compareTo(a.getAvgRating());
                        if (ratingCompare != 0) {
                            return ratingCompare;
                        }
                        return b.getReviewCount().compareTo(a.getReviewCount());
                    })
                    .limit(limit != null ? limit : 10)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 根据酒类推荐酒吧（保留用于兼容）
     * @deprecated 使用 recommendBarsByAlcoholIds 代替
     */
    @Deprecated
    public List<BarVO> recommendBarsByBeverage(Long beverageId, Double userLatitude, Double userLongitude, Integer limit) {
        Beverage beverage = beverageMapper.selectById(beverageId);
        if (beverage == null) {
            throw new BusinessException("酒类不存在");
        }

        // 获取所有活跃且有经纬度信息的酒吧
        LambdaQueryWrapper<Bar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bar::getIsActive, true)
                .isNotNull(Bar::getLatitude)
                .isNotNull(Bar::getLongitude);

        // 如果用户提供了位置信息，优先推荐附近的酒吧
        if (userLatitude != null && userLongitude != null) {
            List<Bar> allBars = barMapper.selectList(wrapper);
            
            // 转换为VO
            List<BarVO> barVOs = allBars.stream()
                    .map(this::convertBarToVO)
                    .collect(Collectors.toList());

            // 使用BarRankingAlgorithm进行排序
            BarRankingAlgorithm.UserLocation userLocation = 
                    new BarRankingAlgorithm.UserLocation(userLatitude, userLongitude);
            
            List<BarRankingAlgorithm.BarRecommendationResult> results = 
                    BarRankingAlgorithm.rankBars(barVOs, userLocation, null);

            // 提取BarVO并设置距离和分数
            List<BarVO> recommendedBars = results.stream()
                    .map(result -> {
                        BarVO bar = result.getBar();
                        bar.setDistance(result.getDistance());
                        bar.setScore(result.getCompositeScore());
                        return bar;
                    })
                    .limit(limit != null ? limit : 10)
                    .collect(Collectors.toList());

            return recommendedBars;
        } else {
            // 如果没有位置信息，按评分排序
            wrapper.orderByDesc(Bar::getAvgRating)
                   .orderByDesc(Bar::getReviewCount);
            
            Page<Bar> page = new Page<>(1, limit != null ? limit : 10);
            List<Bar> bars = barMapper.selectPage(page, wrapper).getRecords();
            
            return bars.stream()
                    .map(this::convertBarToVO)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 搜索酒吧（模糊搜索）
     */
    public List<BarVO> searchBars(String keyword, Integer limit) {
        LambdaQueryWrapper<Bar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bar::getIsActive, true);
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Bar::getName, keyword)
                             .or()
                             .like(Bar::getAddress, keyword)
                             .or()
                             .like(Bar::getMainBeverages, keyword));
        }
        
        wrapper.orderByDesc(Bar::getAvgRating)
               .orderByDesc(Bar::getReviewCount);
        
        Page<Bar> page = new Page<>(1, limit != null ? limit : 20);
        List<Bar> bars = barMapper.selectPage(page, wrapper).getRecords();
        
        return bars.stream()
                .map(this::convertBarToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取我发起的活动
     */
    public PageResult<ActivityVO> getMyCreatedActivities(Long userId, int page, int size) {
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getOrganizerId, userId)
               .eq(Activity::getIsActive, true)
               .orderByDesc(Activity::getCreatedAt);
        
        Page<Activity> result = activityMapper.selectPage(pageParam, wrapper);
        List<ActivityVO> vos = result.getRecords().stream()
                .map(activity -> convertToVO(activity, userId))
                .collect(Collectors.toList());
        
        return new PageResult<>(result.getTotal(), page, size, vos);
    }

    /**
     * 获取推荐的活动（已审核通过的活动）
     * @param timeRange 时间范围：THREE_DAYS(最近三天), ONE_MONTH(最近一个月), ONE_YEAR(最近一年), ALL(全部)
     */
    public PageResult<ActivityVO> getRecommendedActivities(Long userId, Long barId, Long beverageId, String timeRange, int page, int size) {
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getReviewStatus, ReviewStatus.APPROVED)
               .eq(Activity::getIsActive, true)
               .ne(Activity::getStatus, ActivityStatus.CANCELLED);
        
        // 根据时间范围筛选（只显示未来活动）
        LocalDateTime now = LocalDateTime.now();
        wrapper.ge(Activity::getActivityTime, now); // 只显示未来活动
        
        if (timeRange != null && !"ALL".equals(timeRange)) {
            switch (timeRange) {
                case "THREE_DAYS":
                    wrapper.le(Activity::getActivityTime, now.plusDays(3)); // 未来三天内
                    break;
                case "ONE_MONTH":
                    wrapper.le(Activity::getActivityTime, now.plusMonths(1)); // 未来一个月内
                    break;
                case "ONE_YEAR":
                    wrapper.le(Activity::getActivityTime, now.plusYears(1)); // 未来一年内
                    break;
                case "ALL":
                default:
                    // 显示所有活动（包括已结束的）
                    break;
            }
        } else {
            // 默认只显示未来的活动
            wrapper.ge(Activity::getActivityTime, now)
                   .ne(Activity::getStatus, ActivityStatus.FINISHED);
        }
        
        if (barId != null) {
            wrapper.eq(Activity::getBarId, barId);
        }
        if (beverageId != null) {
            wrapper.eq(Activity::getBeverageId, beverageId);
        }
        
        wrapper.orderByDesc(Activity::getActivityTime); // 按时间降序（最新的在前）
        
        Page<Activity> result = activityMapper.selectPage(pageParam, wrapper);
        List<ActivityVO> vos = result.getRecords().stream()
                .map(activity -> convertToVO(activity, userId))
                .collect(Collectors.toList());
        
        return new PageResult<>(result.getTotal(), page, size, vos);
    }

    /**
     * 获取我参与的活动
     */
    public PageResult<ActivityVO> getMyParticipatedActivities(Long userId, int page, int size) {
        // 先获取用户参与的活动ID列表
        LambdaQueryWrapper<ActivityParticipant> participantWrapper = new LambdaQueryWrapper<>();
        participantWrapper.eq(ActivityParticipant::getUserId, userId);
        List<ActivityParticipant> participants = activityParticipantMapper.selectList(participantWrapper);
        
        if (participants.isEmpty()) {
            return new PageResult<>(0L, page, size, List.of());
        }
        
        List<Long> activityIds = participants.stream()
                .map(ActivityParticipant::getActivityId)
                .collect(Collectors.toList());
        
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Activity::getId, activityIds)
               .eq(Activity::getIsActive, true)
               .orderByDesc(Activity::getActivityTime);
        
        Page<Activity> result = activityMapper.selectPage(pageParam, wrapper);
        List<ActivityVO> vos = result.getRecords().stream()
                .map(activity -> convertToVO(activity, userId))
                .collect(Collectors.toList());
        
        return new PageResult<>(result.getTotal(), page, size, vos);
    }

    /**
     * 获取活动详情
     */
    public ActivityVO getActivityById(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getIsActive()) {
            throw new BusinessException("活动不存在");
        }
        return convertToVO(activity, userId);
    }

    /**
     * 参与活动
     */
    @Transactional
    public void joinActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getIsActive()) {
            throw new BusinessException("活动不存在");
        }

        if (!ReviewStatus.APPROVED.equals(activity.getReviewStatus())) {
            throw new BusinessException("活动尚未审核通过");
        }

        if (ActivityStatus.CANCELLED.equals(activity.getStatus()) || 
            ActivityStatus.FINISHED.equals(activity.getStatus())) {
            throw new BusinessException("活动已取消或已结束");
        }

        // 检查是否已参与
        LambdaQueryWrapper<ActivityParticipant> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ActivityParticipant::getActivityId, activityId)
                   .eq(ActivityParticipant::getUserId, userId);
        if (activityParticipantMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("您已经参与此活动");
        }

        // 检查人数上限
        LambdaQueryWrapper<ActivityParticipant> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(ActivityParticipant::getActivityId, activityId);
        int currentCount = activityParticipantMapper.selectCount(countWrapper).intValue();
        if (currentCount >= activity.getMaxParticipants()) {
            throw new BusinessException("活动人数已满");
        }

        ActivityParticipant participant = new ActivityParticipant();
        participant.setActivityId(activityId);
        participant.setUserId(userId);
        activityParticipantMapper.insert(participant);

        log.info("User {} joined activity {}", userId, activityId);
    }

    /**
     * 取消参与活动
     */
    @Transactional
    public void cancelJoinActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getIsActive()) {
            throw new BusinessException("活动不存在");
        }

        // 发起者不能取消参与
        if (activity.getOrganizerId().equals(userId)) {
            throw new BusinessException("发起者不能取消参与");
        }

        // 检查是否已参与
        LambdaQueryWrapper<ActivityParticipant> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ActivityParticipant::getActivityId, activityId)
                   .eq(ActivityParticipant::getUserId, userId);
        if (activityParticipantMapper.selectCount(checkWrapper) == 0) {
            throw new BusinessException("您未参与此活动");
        }

        activityParticipantMapper.delete(checkWrapper);

        log.info("User {} cancelled participation in activity {}", userId, activityId);
    }

    /**
     * 取消活动（发起者）
     */
    @Transactional
    public void cancelActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getIsActive()) {
            throw new BusinessException("活动不存在");
        }

        // 只有发起者可以取消活动
        if (!activity.getOrganizerId().equals(userId)) {
            throw new BusinessException("只有发起者可以取消活动");
        }

        // 检查活动状态，已结束或已取消的活动不能再次取消
        if (ActivityStatus.CANCELLED.equals(activity.getStatus()) || 
            ActivityStatus.FINISHED.equals(activity.getStatus())) {
            throw new BusinessException("活动已取消或已结束，无法再次取消");
        }
        
        // 已拒绝的活动也不能取消
        if (ReviewStatus.REJECTED.equals(activity.getReviewStatus())) {
            throw new BusinessException("活动已被拒绝，无法取消");
        }

        // 更新活动状态为已取消
        LambdaUpdateWrapper<Activity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Activity::getId, activityId)
                .set(Activity::getStatus, ActivityStatus.CANCELLED);
        activityMapper.update(null, wrapper);

        log.info("User {} cancelled activity {}", userId, activityId);
    }

    /**
     * 审核活动（管理员）
     */
    @Transactional
    public void reviewActivity(Long activityId, ReviewActivityRequest request, Long reviewerId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        if (!ReviewStatus.PENDING.equals(activity.getReviewStatus())) {
            throw new BusinessException("活动已审核");
        }

        if (ReviewStatus.REJECTED.equals(request.getStatus()) && 
            (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty())) {
            throw new BusinessException("拒绝审核时必须填写拒绝原因");
        }

        LambdaUpdateWrapper<Activity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Activity::getId, activityId)
                .set(Activity::getReviewStatus, request.getStatus())
                .set(Activity::getReviewedBy, reviewerId)
                .set(Activity::getReviewedAt, LocalDateTime.now());

        if (ReviewStatus.APPROVED.equals(request.getStatus())) {
            wrapper.set(Activity::getStatus, ActivityStatus.APPROVED);
        } else if (ReviewStatus.REJECTED.equals(request.getStatus())) {
            wrapper.set(Activity::getStatus, ActivityStatus.CANCELLED)
                   .set(Activity::getRejectReason, request.getRejectReason());
        }

        activityMapper.update(null, wrapper);

        log.info("Admin {} reviewed activity {}, status: {}", reviewerId, activityId, request.getStatus());
    }

    /**
     * 获取待审核的活动列表（管理员）
     */
    public PageResult<ActivityVO> getPendingActivities(int page, int size) {
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getReviewStatus, ReviewStatus.PENDING)
               .eq(Activity::getIsActive, true)
               .orderByAsc(Activity::getCreatedAt);
        
        Page<Activity> result = activityMapper.selectPage(pageParam, wrapper);
        List<ActivityVO> vos = result.getRecords().stream()
                .map(activity -> convertToVO(activity, null))
                .collect(Collectors.toList());
        
        return new PageResult<>(result.getTotal(), page, size, vos);
    }

    /**
     * 更新活动状态（定时任务调用）
     */
    @Transactional
    public void updateActivityStatus() {
        LocalDateTime now = LocalDateTime.now();
        
        // 更新进行中的活动
        LambdaUpdateWrapper<Activity> ongoingWrapper = new LambdaUpdateWrapper<>();
        ongoingWrapper.eq(Activity::getStatus, ActivityStatus.APPROVED)
                     .eq(Activity::getReviewStatus, ReviewStatus.APPROVED)
                     .le(Activity::getActivityTime, now)
                     .set(Activity::getStatus, ActivityStatus.ONGOING);
        activityMapper.update(null, ongoingWrapper);
        
        // 更新已结束的活动（假设活动持续2小时）
        LocalDateTime twoHoursAgo = now.minusHours(2);
        LambdaUpdateWrapper<Activity> finishedWrapper = new LambdaUpdateWrapper<>();
        finishedWrapper.in(Activity::getStatus, ActivityStatus.ONGOING, ActivityStatus.APPROVED)
                      .le(Activity::getActivityTime, twoHoursAgo)
                      .set(Activity::getStatus, ActivityStatus.FINISHED);
        activityMapper.update(null, finishedWrapper);
    }

    /**
     * 转换为VO
     */
    private ActivityVO convertToVO(Activity activity, Long currentUserId) {
        // 获取发起者信息
        User organizer = userMapper.selectById(activity.getOrganizerId());
        String organizerName = organizer != null ? organizer.getUsername() : "未知用户";
        String organizerAvatar = organizer != null ? organizer.getAvatarUrl() : null;

        // 获取酒类信息
        String beverageName = "未知酒类";
        if (activity.getAlcoholIds() != null && !activity.getAlcoholIds().isEmpty()) {
            // 如果有alcoholIds，根据alcoholIds生成酒类名称
            List<String> alcoholNames = activity.getAlcoholIds().stream()
                    .map(id -> {
                        Alcohol alcohol = alcoholMapper.selectById(id);
                        return alcohol != null ? alcohol.getName() : null;
                    })
                    .filter(name -> name != null)
                    .collect(Collectors.toList());
            if (!alcoholNames.isEmpty()) {
                beverageName = String.join("、", alcoholNames);
            }
        } else if (activity.getBeverageId() != null) {
            // 兼容旧数据，使用beverageId
            Beverage beverage = beverageMapper.selectById(activity.getBeverageId());
            beverageName = beverage != null ? beverage.getName() : "未知酒类";
        }

        // 获取酒吧信息
        String barName = null;
        String barAddress = null;
        if (activity.getBarId() != null) {
            Bar bar = barMapper.selectById(activity.getBarId());
            if (bar != null) {
                barName = bar.getName();
                barAddress = bar.getAddress();
            }
        }

        // 获取当前参与人数
        LambdaQueryWrapper<ActivityParticipant> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(ActivityParticipant::getActivityId, activity.getId());
        int currentParticipants = activityParticipantMapper.selectCount(countWrapper).intValue();

        // 检查当前用户是否已参与
        Boolean isParticipated = false;
        Boolean hasReviewed = false;
        if (currentUserId != null) {
            LambdaQueryWrapper<ActivityParticipant> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(ActivityParticipant::getActivityId, activity.getId())
                       .eq(ActivityParticipant::getUserId, currentUserId);
            ActivityParticipant participant = activityParticipantMapper.selectOne(checkWrapper);
            if (participant != null) {
                isParticipated = true;
                hasReviewed = Boolean.TRUE.equals(participant.getHasReviewed());
            }
        }

        // 检查是否是发起者
        Boolean isOrganizer = currentUserId != null && 
                activity.getOrganizerId().equals(currentUserId);

        // 检查是否已结束
        Boolean isFinished = ActivityStatus.FINISHED.equals(activity.getStatus()) ||
                (activity.getActivityTime() != null && activity.getActivityTime().isBefore(LocalDateTime.now().minusHours(2)));

        return ActivityVO.builder()
                .id(activity.getId())
                .organizerId(activity.getOrganizerId())
                .organizerName(organizerName)
                .organizerAvatar(organizerAvatar)
                .activityTime(activity.getActivityTime())
                .beverageId(activity.getBeverageId())
                .beverageName(beverageName)
                .barId(activity.getBarId())
                .barName(barName)
                .barAddress(barAddress)
                .maxParticipants(activity.getMaxParticipants())
                .currentParticipants(currentParticipants)
                .remark(activity.getRemark())
                .reviewStatus(activity.getReviewStatus())
                .status(activity.getStatus())
                .createdAt(activity.getCreatedAt())
                .rejectReason(activity.getRejectReason())
                .isParticipated(isParticipated)
                .isOrganizer(isOrganizer)
                .isFinished(isFinished)
                .hasReviewed(hasReviewed)
                .build();
    }

    /**
     * 活动结束后评价酒吧
     */
    @Transactional
    public void reviewActivityBar(Long activityId, ActivityBarReviewRequest request, Long userId) {
        // 检查活动是否存在
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getIsActive()) {
            throw new BusinessException("活动不存在");
        }

        // 检查活动是否已结束
        if (!ActivityStatus.FINISHED.equals(activity.getStatus())) {
            // 检查活动时间是否已过（活动时间后2小时视为结束）
            LocalDateTime now = LocalDateTime.now();
            if (activity.getActivityTime() == null || 
                activity.getActivityTime().plusHours(2).isAfter(now)) {
                throw new BusinessException("活动尚未结束，无法评价");
            }
        }

        // 检查活动是否关联了酒吧
        if (activity.getBarId() == null) {
            throw new BusinessException("该活动没有关联酒吧，无法评价");
        }

        // 检查用户是否参与了该活动
        LambdaQueryWrapper<ActivityParticipant> participantWrapper = new LambdaQueryWrapper<>();
        participantWrapper.eq(ActivityParticipant::getActivityId, activityId)
                         .eq(ActivityParticipant::getUserId, userId);
        ActivityParticipant participant = activityParticipantMapper.selectOne(participantWrapper);
        
        if (participant == null) {
            throw new BusinessException("您未参与此活动，无法评价");
        }

        // 检查用户是否已经评价过
        if (Boolean.TRUE.equals(participant.getHasReviewed())) {
            throw new BusinessException("您已经评价过此活动");
        }

        // 检查用户是否已经评价过该酒吧
        LambdaQueryWrapper<BarReview> barReviewWrapper = new LambdaQueryWrapper<>();
        barReviewWrapper.eq(BarReview::getBarId, activity.getBarId())
                       .eq(BarReview::getUserId, userId)
                       .eq(BarReview::getIsActive, true);
        BarReview existingBarReview = barReviewMapper.selectOne(barReviewWrapper);
        
        if (existingBarReview != null) {
            // 如果已经评价过酒吧，只更新参与者状态
            participant.setHasReviewed(true);
            activityParticipantMapper.updateById(participant);
            throw new BusinessException("您已经评价过该酒吧");
        }

        // 创建酒吧评价
        BarReview barReview = new BarReview();
        barReview.setBarId(activity.getBarId());
        barReview.setUserId(userId);
        barReview.setRating(request.getRating());
        barReview.setContent(request.getContent());
        barReviewMapper.insert(barReview);

        // 更新酒吧的平均评分和评价数量
        updateBarRating(activity.getBarId());

        // 更新参与者评价状态
        participant.setHasReviewed(true);
        activityParticipantMapper.updateById(participant);

        log.info("User {} reviewed bar {} after activity {}", userId, activity.getBarId(), activityId);
    }

    /**
     * 更新酒吧的平均评分和评价数量
     */
    private void updateBarRating(Long barId) {
        LambdaQueryWrapper<BarReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BarReview::getBarId, barId)
               .eq(BarReview::getIsActive, true);
        
        List<BarReview> reviews = barReviewMapper.selectList(wrapper);
        
        if (reviews.isEmpty()) {
            return;
        }
        
        double avgRating = reviews.stream()
                .mapToInt(BarReview::getRating)
                .average()
                .orElse(0.0);
        
        int reviewCount = reviews.size();
        
        Bar bar = barMapper.selectById(barId);
        if (bar != null) {
            bar.setAvgRating(avgRating);
            bar.setReviewCount(reviewCount);
            barMapper.updateById(bar);
        }
    }

    /**
     * 获取活动参与者列表
     */
    public List<SimpleUserVO> getActivityParticipants(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getIsActive()) {
            throw new BusinessException("活动不存在");
        }

        LambdaQueryWrapper<ActivityParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityParticipant::getActivityId, activityId)
               .orderByAsc(ActivityParticipant::getJoinedAt);
        List<ActivityParticipant> participants = activityParticipantMapper.selectList(wrapper);

        return participants.stream()
                .map(participant -> {
                    User user = userMapper.selectById(participant.getUserId());
                    if (user == null) {
                        return null;
                    }
                    return SimpleUserVO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .avatarUrl(user.getAvatarUrl())
                            .bio(user.getBio())
                            .build();
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());
    }

    /**
     * 转换Bar为VO
     */
    private BarVO convertBarToVO(Bar bar) {
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
}

