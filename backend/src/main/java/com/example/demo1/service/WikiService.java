package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.common.enums.UserRole;
import com.example.demo1.common.enums.WikiStatus;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.dto.request.WikiDiscussionRequest;
import com.example.demo1.dto.request.WikiPageRequest;
import com.example.demo1.dto.response.WikiDiscussionVO;
import com.example.demo1.dto.response.WikiPageVO;
import com.example.demo1.dto.response.WikiRevisionVO;
import com.example.demo1.dto.response.WikiStatsVO;
import com.example.demo1.entity.User;
import com.example.demo1.entity.WikiDiscussion;
import com.example.demo1.entity.WikiPage;
import com.example.demo1.entity.WikiRevision;
import com.example.demo1.mapper.WikiDiscussionMapper;
import com.example.demo1.mapper.WikiPageMapper;
import com.example.demo1.mapper.WikiRevisionMapper;
import com.example.demo1.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiService {

    private final WikiPageMapper wikiPageMapper;
    private final WikiRevisionMapper wikiRevisionMapper;
    private final WikiDiscussionMapper wikiDiscussionMapper;
    private final UserService userService;
    private final CollectionService collectionService;

    public PageResult<WikiPageVO> listPages(String keyword, int page, int pageSize) {
        return listPages(keyword, page, pageSize, null);
    }

    /**
     * 列出Wiki页面
     * 非管理员用户只能看到已发布的页面
     * 
     * @param keyword 搜索关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @param currentUserId 当前用户ID（用于判断是否为管理员）
     * @return 页面结果
     */
    public PageResult<WikiPageVO> listPages(String keyword, int page, int pageSize, Long currentUserId) {
        LambdaQueryWrapper<WikiPage> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(WikiPage::getTitle, keyword)
                .or()
                .like(WikiPage::getSummary, keyword);
        }
        
        // 非管理员用户只能看到已发布的页面
        if (currentUserId != null) {
            User currentUser = userService.getRequiredUser(currentUserId);
            if (!UserRole.ADMIN.equals(currentUser.getRole())) {
                wrapper.eq(WikiPage::getStatus, WikiStatus.PUBLISHED);
            }
        } else {
            // 未登录用户只能看到已发布的页面
            wrapper.eq(WikiPage::getStatus, WikiStatus.PUBLISHED);
        }
        
        wrapper.orderByDesc(WikiPage::getUpdatedAt);
        Page<WikiPage> mpPage = wikiPageMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(mpPage.getTotal(), (int) mpPage.getCurrent(), (int) mpPage.getSize(),
            mpPage.getRecords().stream().map(pageEntity -> toVo(pageEntity, currentUserId)).toList());
    }

    /**
     * 获取Wiki页面详情
     * 非管理员用户无法访问未审核的页面
     * 
     * @param slugOrId slug或ID
     * @param currentUserId 当前用户ID（用于判断是否为管理员）
     * @return 页面VO
     */
    public WikiPageVO getPage(String slugOrId, Long currentUserId) {
        WikiPage page = findPage(slugOrId);
        if (page == null) {
            throw new BusinessException(404, "词条不存在");
        }
        
        // 非管理员用户无法访问未审核的页面
        if (currentUserId != null) {
            User currentUser = userService.getRequiredUser(currentUserId);
            if (!UserRole.ADMIN.equals(currentUser.getRole()) 
                && !WikiStatus.PUBLISHED.equals(page.getStatus())) {
                throw new BusinessException(404, "词条不存在或正在审核中");
            }
        } else {
            // 未登录用户无法访问未审核的页面
            if (!WikiStatus.PUBLISHED.equals(page.getStatus())) {
                throw new BusinessException(404, "词条不存在或正在审核中");
            }
        }
        
        return toVo(page, currentUserId);
    }

    /**
     * 创建Wiki页面
     * 
     * 设计说明：
     * - 管理员创建的词条直接发布（PUBLISHED）
     * - 非管理员创建的词条需要审核（UNDER_REVIEW）
     * - slug 基于 title 自动生成，确保唯一性
     * - 如果title已存在，抛出异常并返回已存在的wiki信息
     * 
     * @param editorId 编辑者ID（当前登录用户）
     * @param request 页面请求数据
     * @return 创建的页面VO
     * @throws BusinessException 如果title已存在，异常信息包含已存在的wiki信息
     */
    @Transactional
    public WikiPageVO createPage(Long editorId, WikiPageRequest request) {
        User editor = userService.getRequiredUser(editorId);
        
        // 检查title是否已存在
        WikiPage existingPage = wikiPageMapper.selectOne(
            new LambdaQueryWrapper<WikiPage>().eq(WikiPage::getTitle, request.getTitle())
        );
        if (existingPage != null) {
            // 抛出异常，包含已存在的wiki信息
            Map<String, Object> existingWikiInfo = new HashMap<>();
            existingWikiInfo.put("id", existingPage.getId());
            existingWikiInfo.put("slug", existingPage.getSlug());
            existingWikiInfo.put("title", existingPage.getTitle());
            
            Map<String, Object> data = new HashMap<>();
            data.put("existingWiki", existingWikiInfo);
            
            throw new BusinessException(409, "该词条已存在", data);
        }
        
        String slug = generateUniqueSlug(request.getTitle());
        
        WikiPage page = new WikiPage();
        page.setSlug(slug);
        page.setTitle(request.getTitle());
        page.setSummary(request.getSummary());
        page.setContent(request.getContent());
        
        // 根据用户角色设置状态
        if (UserRole.ADMIN.equals(editor.getRole())) {
            // 管理员直接发布
            page.setStatus(WikiStatus.PUBLISHED);
        } else {
            // 非管理员需要审核
            page.setStatus(WikiStatus.UNDER_REVIEW);
        }
        
        page.setLastEditorId(editor.getId());
        page.setLastEditorName(editor.getUsername());
        
        wikiPageMapper.insert(page);
        log.info("创建Wiki页面: id={}, slug={}, title={}, editor={}, status={}", 
            page.getId(), slug, request.getTitle(), editor.getUsername(), page.getStatus());
        
        String revisionSummary = StringUtils.isNotBlank(request.getEditSummary()) 
            ? request.getEditSummary() 
            : "创建词条";
        recordRevision(page, editor, revisionSummary);
        
        return toVo(page, editorId);
    }

    /**
     * 更新Wiki页面
     * 
     * 设计说明：
     * - 管理员更新后保持原状态或直接发布
     * - 非管理员更新后需要重新审核（UNDER_REVIEW）
     * 
     * @param id 页面ID
     * @param editorId 编辑者ID（当前登录用户）
     * @param request 页面请求数据
     * @return 更新的页面VO
     */
    @Transactional
    public WikiPageVO updatePage(Long id, Long editorId, WikiPageRequest request) {
        WikiPage existing = wikiPageMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "Wiki页面不存在");
        }
        User editor = userService.getRequiredUser(editorId);
        existing.setTitle(request.getTitle());
        existing.setSummary(request.getSummary());
        existing.setContent(request.getContent());
        
        // 根据用户角色设置状态
        if (UserRole.ADMIN.equals(editor.getRole())) {
            // 管理员更新后保持已发布状态（如果原来是已发布）
            if (!WikiStatus.PUBLISHED.equals(existing.getStatus())) {
                existing.setStatus(WikiStatus.PUBLISHED);
            }
        } else {
            // 非管理员更新后需要重新审核
            existing.setStatus(WikiStatus.UNDER_REVIEW);
        }
        
        existing.setLastEditorId(editor.getId());
        existing.setLastEditorName(editor.getUsername());
        wikiPageMapper.updateById(existing);
        
        log.info("更新Wiki页面: id={}, title={}, editor={}, status={}", 
            id, request.getTitle(), editor.getUsername(), existing.getStatus());
        
        String revisionSummary = StringUtils.isNotBlank(request.getEditSummary()) 
            ? request.getEditSummary() 
            : "更新词条";
        recordRevision(existing, editor, revisionSummary);
        return toVo(existing, editorId);
    }

    public void ensureDefaultPage(String slug, String title, String summary, String content) {
        WikiPage existing = wikiPageMapper.selectOne(new LambdaQueryWrapper<WikiPage>().eq(WikiPage::getSlug, slug));
        if (existing != null) {
            return;
        }
        WikiPage page = new WikiPage();
        page.setSlug(slug);
        page.setTitle(title);
        page.setSummary(summary);
        page.setContent(content);
        page.setStatus(WikiStatus.PUBLISHED);
        page.setLastEditorName("系统");
        wikiPageMapper.insert(page);
        recordRevision(page, null, "系统初始化");
    }

    public List<WikiRevisionVO> listRevisions(String slugOrId) {
        WikiPage page = findPage(slugOrId);
        if (page == null) {
            throw new BusinessException(404, "词条不存在");
        }
        return wikiRevisionMapper.selectList(new LambdaQueryWrapper<WikiRevision>()
                .eq(WikiRevision::getPageId, page.getId())
                .orderByDesc(WikiRevision::getCreatedAt))
            .stream()
            .map(revision -> WikiRevisionVO.builder()
                .id(revision.getId())
                .editorName(StringUtils.defaultIfBlank(revision.getEditorName(), "系统"))
                .summary(revision.getSummary())
                .createdAt(revision.getCreatedAt())
                .build())
            .toList();
    }

    public WikiRevisionVO getRevisionDetail(String slugOrId, Long revisionId) {
        WikiPage page = findPage(slugOrId);
        if (page == null) {
            throw new BusinessException(404, "词条不存在");
        }
        WikiRevision revision = wikiRevisionMapper.selectById(revisionId);
        if (revision == null || !revision.getPageId().equals(page.getId())) {
            throw new BusinessException(404, "历史版本不存在");
        }
        return WikiRevisionVO.builder()
            .id(revision.getId())
            .editorName(StringUtils.defaultIfBlank(revision.getEditorName(), "系统"))
            .summary(revision.getSummary())
            .createdAt(revision.getCreatedAt())
            .content(revision.getContent())
            .build();
    }

    @Transactional
    public WikiPageVO restoreRevision(String slugOrId, Long revisionId, Long editorId) {
        WikiPage page = findPage(slugOrId);
        if (page == null) {
            throw new BusinessException(404, "词条不存在");
        }
        WikiRevision revision = wikiRevisionMapper.selectById(revisionId);
        if (revision == null || !revision.getPageId().equals(page.getId())) {
            throw new BusinessException(404, "历史版本不存在");
        }
        User editor = userService.getRequiredUser(editorId);
        page.setContent(revision.getContent());
        page.setStatus(WikiStatus.UNDER_REVIEW);
        page.setLastEditorId(editor.getId());
        page.setLastEditorName(editor.getUsername());
        wikiPageMapper.updateById(page);
        recordRevision(page, editor, String.format("恢复至 %s 的版本", revision.getCreatedAt()));
        return toVo(page, editorId);
    }

    public WikiStatsVO getStats() {
        // 只统计已发布的词条
        long entryCount = wikiPageMapper.selectCount(
            new LambdaQueryWrapper<WikiPage>().eq(WikiPage::getStatus, WikiStatus.PUBLISHED)
        );
        long revisionCount = wikiRevisionMapper.selectCount(null);
        Set<Long> contributorIds = wikiRevisionMapper.selectList(new LambdaQueryWrapper<WikiRevision>()
                .select(WikiRevision::getEditorId))
            .stream()
            .map(WikiRevision::getEditorId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
        return WikiStatsVO.builder()
            .entryCount(entryCount)
            .editCount(revisionCount)
            .contributorCount((long) contributorIds.size())
            .build();
    }

    /**
     * 获取待审核的Wiki页面列表（管理员）
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @return 待审核的页面列表
     */
    public PageResult<WikiPageVO> getPendingPages(int page, int pageSize) {
        LambdaQueryWrapper<WikiPage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WikiPage::getStatus, WikiStatus.UNDER_REVIEW)
               .orderByDesc(WikiPage::getUpdatedAt);
        Page<WikiPage> mpPage = wikiPageMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(
            mpPage.getTotal(), 
            (int) mpPage.getCurrent(), 
            (int) mpPage.getSize(),
            mpPage.getRecords().stream().map(pageEntity -> toVo(pageEntity, null)).toList()
        );
    }

    /**
     * 审核Wiki页面（管理员）
     * 
     * @param pageId 页面ID
     * @param status 审核状态（PUBLISHED或保持UNDER_REVIEW）
     * @param reviewerId 审核者ID
     */
    @Transactional
    public void reviewWikiPage(Long pageId, WikiStatus status, Long reviewerId) {
        WikiPage page = wikiPageMapper.selectById(pageId);
        if (page == null) {
            throw new BusinessException(404, "Wiki页面不存在");
        }

        if (!WikiStatus.UNDER_REVIEW.equals(page.getStatus())) {
            throw new BusinessException(400, "该词条不需要审核");
        }

        if (WikiStatus.PUBLISHED.equals(status)) {
            // 审核通过，设置为已发布
            LambdaUpdateWrapper<WikiPage> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(WikiPage::getId, pageId)
                   .set(WikiPage::getStatus, WikiStatus.PUBLISHED);
            wikiPageMapper.update(null, wrapper);
            log.info("管理员 {} 审核通过Wiki页面: id={}, title={}", reviewerId, pageId, page.getTitle());
        } else {
            // 保持待审核状态（可以用于拒绝审核，但当前设计中没有拒绝状态）
            log.info("管理员 {} 保持Wiki页面待审核: id={}, title={}", reviewerId, pageId, page.getTitle());
        }
    }

    @Transactional
    public boolean toggleFavorite(Long pageId, Long userId) {
        WikiPage page = wikiPageMapper.selectById(pageId);
        if (page == null) {
            throw new BusinessException(404, "词条不存在");
        }
        return collectionService.toggleWikiFavorite(userId, page);
    }

    public List<WikiDiscussionVO> listDiscussions(String slugOrId) {
        WikiPage page = findPage(slugOrId);
        if (page == null) {
            throw new BusinessException(404, "词条不存在");
        }
        List<WikiDiscussion> discussions = wikiDiscussionMapper.selectList(new LambdaQueryWrapper<WikiDiscussion>()
            .eq(WikiDiscussion::getPageId, page.getId())
            .orderByDesc(WikiDiscussion::getCreatedAt));
        if (discussions.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Map<Long, User> userMap = userService.mapByIds(
            discussions.stream().map(WikiDiscussion::getUserId).collect(Collectors.toList()));
        return discussions.stream()
            .map(discussion -> WikiDiscussionVO.builder()
                .id(discussion.getId())
                .author(userService.buildSimpleUser(userMap.get(discussion.getUserId())))
                .content(discussion.getContent())
                .createdAt(discussion.getCreatedAt())
                .build())
            .toList();
    }

    @Transactional
    public WikiDiscussionVO createDiscussion(String slugOrId, Long userId, WikiDiscussionRequest request) {
        WikiPage page = findPage(slugOrId);
        if (page == null) {
            throw new BusinessException(404, "词条不存在");
        }
        User author = userService.getRequiredUser(userId);
        WikiDiscussion discussion = new WikiDiscussion();
        discussion.setPageId(page.getId());
        discussion.setUserId(userId);
        discussion.setContent(request.getContent());
        wikiDiscussionMapper.insert(discussion);
        return WikiDiscussionVO.builder()
            .id(discussion.getId())
            .author(userService.buildSimpleUser(author))
            .content(discussion.getContent())
            .createdAt(discussion.getCreatedAt())
            .build();
    }

    private WikiPage findPage(String slugOrId) {
        if (StringUtils.isNumeric(slugOrId)) {
            WikiPage page = wikiPageMapper.selectById(Long.parseLong(slugOrId));
            if (page != null) {
                return page;
            }
        }
        return wikiPageMapper.selectOne(new LambdaQueryWrapper<WikiPage>().eq(WikiPage::getSlug, slugOrId));
    }

    private String generateUniqueSlug(String title) {
        String slug = SlugUtils.toSlug(title);
        if (StringUtils.isBlank(slug)) {
            slug = "wiki-" + System.currentTimeMillis();
        }
        String candidate = slug;
        int counter = 1;
        while (wikiPageMapper.selectCount(new LambdaQueryWrapper<WikiPage>().eq(WikiPage::getSlug, candidate)) > 0) {
            candidate = slug + "-" + counter++;
        }
        return candidate;
    }

    private WikiPageVO toVo(WikiPage page, Long currentUserId) {
        boolean favorited = collectionService.isWikiFavorited(currentUserId, page.getId());
        return WikiPageVO.builder()
            .id(page.getId())
            .slug(page.getSlug())
            .title(page.getTitle())
            .summary(page.getSummary())
            .content(page.getContent())
            .status(page.getStatus())
            .lastEditorName(page.getLastEditorName())
            .updatedAt(page.getUpdatedAt())
            .favorited(favorited)
            .build();
    }

    private void recordRevision(WikiPage page, User editor, String summary) {
        WikiRevision revision = new WikiRevision();
        revision.setPageId(page.getId());
        if (editor != null) {
            revision.setEditorId(editor.getId());
            revision.setEditorName(editor.getUsername());
        } else {
            revision.setEditorName("系统");
        }
        revision.setSummary(summary);
        revision.setContent(page.getContent());
        wikiRevisionMapper.insert(revision);
    }
}
