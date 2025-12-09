package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo1.common.enums.WikiStatus;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.dto.request.WikiPageRequest;
import com.example.demo1.dto.response.WikiPageVO;
import com.example.demo1.entity.User;
import com.example.demo1.entity.WikiPage;
import com.example.demo1.mapper.WikiPageMapper;
import com.example.demo1.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WikiService {

    private final WikiPageMapper wikiPageMapper;
    private final UserService userService;

    public PageResult<WikiPageVO> listPages(String keyword, int page, int pageSize) {
        LambdaQueryWrapper<WikiPage> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(WikiPage::getTitle, keyword)
                    .or()
                    .like(WikiPage::getSummary, keyword);
        }
        wrapper.orderByDesc(WikiPage::getUpdatedAt);
        Page<WikiPage> mpPage = wikiPageMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(mpPage.getTotal(), (int) mpPage.getCurrent(), (int) mpPage.getSize(),
                mpPage.getRecords().stream().map(this::toVo).toList());
    }

    public WikiPageVO getPage(String slugOrId) {
        WikiPage page = findPage(slugOrId);
        if (page == null) {
            throw new BusinessException(404, "未通过的信息时是无");
        }
        return toVo(page);
    }

    @Transactional
    public WikiPageVO createPage(Long editorId, WikiPageRequest request) {
        User editor = userService.getRequiredUser(editorId);
        String slug = generateUniqueSlug(request.getTitle());
        WikiPage page = new WikiPage();
        page.setSlug(slug);
        page.setTitle(request.getTitle());
        page.setSummary(request.getSummary());
        page.setContent(request.getContent());
        page.setStatus(WikiStatus.UNDER_REVIEW);
        page.setLastEditorId(editor.getId());
        page.setLastEditorName(editor.getUsername());
        wikiPageMapper.insert(page);
        return toVo(page);
    }

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
        existing.setStatus(WikiStatus.UNDER_REVIEW);
        existing.setLastEditorId(editor.getId());
        existing.setLastEditorName(editor.getUsername());
        wikiPageMapper.updateById(existing);
        return toVo(existing);
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

    private WikiPageVO toVo(WikiPage page) {
        return WikiPageVO.builder()
                .id(page.getId())
                .slug(page.getSlug())
                .title(page.getTitle())
                .summary(page.getSummary())
                .content(page.getContent())
                .status(page.getStatus())
                .lastEditorName(page.getLastEditorName())
                .updatedAt(page.getUpdatedAt())
                .build();
    }
}
