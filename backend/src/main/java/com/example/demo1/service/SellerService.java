package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo1.common.enums.SellerApplicationStatus;
import com.example.demo1.common.enums.UserRole;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.dto.request.SellerApplicationRequest;
import com.example.demo1.dto.response.SellerApplicationVO;
import com.example.demo1.entity.SellerApplication;
import com.example.demo1.entity.User;
import com.example.demo1.mapper.SellerApplicationMapper;
import com.example.demo1.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerApplicationMapper sellerApplicationMapper;
    private final UserMapper userMapper;

    /**
     * 提交商家认证申请
     */
    @Transactional
    public Long submitApplication(SellerApplicationRequest request, Long userId) {
        // 1. 检查用户是否已经是商家
        User user = userMapper.selectById(userId);
        if (user.getRole() == UserRole.SELLER) {
            throw new BusinessException("您已经是认证商家，无需重复申请");
        }

        // 2. 检查是否有待审核的申请
        LambdaQueryWrapper<SellerApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SellerApplication::getUserId, userId)
                .eq(SellerApplication::getStatus, SellerApplicationStatus.PENDING)
                .eq(SellerApplication::getIsActive, true);
        
        if (sellerApplicationMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("您已有正在审核中的申请，请耐心等待");
        }

        // 3. 创建新申请
        SellerApplication application = new SellerApplication();
        application.setUserId(userId);
        application.setRealName(request.getRealName());
        application.setIdCardNumber(request.getIdCardNumber());
        application.setLicenseImageUrl(request.getLicenseImageUrl());
        application.setStatus(SellerApplicationStatus.PENDING);
        
        sellerApplicationMapper.insert(application);
        return application.getId();
    }

    /**
     * 获取当前用户的申请记录
     */
    public List<SellerApplicationVO> getMyApplications(Long userId) {
        LambdaQueryWrapper<SellerApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SellerApplication::getUserId, userId)
                .eq(SellerApplication::getIsActive, true)
                .orderByDesc(SellerApplication::getCreatedAt);

        return sellerApplicationMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取待审核的申请列表（管理员）
     */
    public List<SellerApplicationVO> getPendingApplications() {
        LambdaQueryWrapper<SellerApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SellerApplication::getStatus, SellerApplicationStatus.PENDING)
                .eq(SellerApplication::getIsActive, true)
                .orderByDesc(SellerApplication::getCreatedAt);

        return sellerApplicationMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 审核申请
     */
    @Transactional
    public void reviewApplication(Long applicationId, SellerApplicationStatus status, String reviewNote, Long reviewerId) {
        SellerApplication application = sellerApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        
        if (application.getStatus() != SellerApplicationStatus.PENDING) {
            throw new BusinessException("该申请已处理");
        }

        // 更新申请状态
        application.setStatus(status);
        application.setReviewNote(reviewNote);
        application.setReviewedBy(reviewerId);
        application.setReviewedAt(LocalDateTime.now());
        sellerApplicationMapper.updateById(application);

        // 如果审核通过，更新用户角色
        if (status == SellerApplicationStatus.APPROVED) {
            User user = userMapper.selectById(application.getUserId());
            if (user != null && user.getRole() == UserRole.USER) {
                user.setRole(UserRole.SELLER);
                userMapper.updateById(user);
                log.info("用户 {} 已升级为商家", user.getId());
            }
        }
    }

    private SellerApplicationVO convertToVO(SellerApplication app) {
        return SellerApplicationVO.builder()
                .id(app.getId())
                .userId(app.getUserId())
                .realName(app.getRealName())
                .idCardNumber(app.getIdCardNumber())
                .licenseImageUrl(app.getLicenseImageUrl())
                .status(app.getStatus())
                .reviewNote(app.getReviewNote())
                .createdAt(app.getCreatedAt())
                .reviewedAt(app.getReviewedAt())
                .build();
    }
}
