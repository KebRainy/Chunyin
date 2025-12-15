package com.example.demo1.controller;

import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.common.response.PageResult;
import com.example.demo1.common.response.Result;
import com.example.demo1.dto.request.WikiDiscussionRequest;
import com.example.demo1.dto.request.WikiPageRequest;
import com.example.demo1.dto.response.WikiDiscussionVO;
import com.example.demo1.dto.response.WikiPageVO;
import com.example.demo1.dto.response.WikiRevisionVO;
import com.example.demo1.dto.response.WikiStatsVO;
import com.example.demo1.security.UserPrincipal;
import com.example.demo1.service.WikiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wiki/pages")
@RequiredArgsConstructor
public class WikiController {

    private final WikiService wikiService;

    @GetMapping
    public Result<PageResult<WikiPageVO>> list(@AuthenticationPrincipal UserPrincipal principal,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String keyword) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return Result.success(wikiService.listPages(keyword, page, pageSize, currentUserId));
    }

    @GetMapping("/{slug}")
    public Result<WikiPageVO> detail(@PathVariable String slug,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return Result.success(wikiService.getPage(slug, currentUserId));
    }

    @GetMapping("/{slug}/history")
    public Result<List<WikiRevisionVO>> history(@PathVariable String slug) {
        return Result.success(wikiService.listRevisions(slug));
    }

    @GetMapping("/{slug}/history/{revisionId}")
    public Result<WikiRevisionVO> revisionDetail(@PathVariable String slug,
                                                 @PathVariable Long revisionId) {
        return Result.success(wikiService.getRevisionDetail(slug, revisionId));
    }

    @PostMapping("/{slug}/history/{revisionId}/restore")
    public Result<WikiPageVO> restoreRevision(@PathVariable String slug,
                                              @PathVariable Long revisionId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(wikiService.restoreRevision(slug, revisionId, principal.getId()));
    }

    @GetMapping("/{slug}/discussions")
    public Result<List<WikiDiscussionVO>> discussions(@PathVariable String slug) {
        return Result.success(wikiService.listDiscussions(slug));
    }

    @PostMapping("/{slug}/discussions")
    public Result<WikiDiscussionVO> createDiscussion(@PathVariable String slug,
                                                     @AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody WikiDiscussionRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(wikiService.createDiscussion(slug, principal.getId(), request));
    }

    @GetMapping("/stats")
    public Result<WikiStatsVO> stats() {
        return Result.success(wikiService.getStats());
    }

    @PostMapping
    public Result<WikiPageVO> create(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody WikiPageRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(wikiService.createPage(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public Result<WikiPageVO> update(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody WikiPageRequest request) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(wikiService.updatePage(id, principal.getId(), request));
    }

    @PostMapping("/{id}/favorite")
    public Result<Boolean> toggleFavorite(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(wikiService.toggleFavorite(id, principal.getId()));
    }
}
