package kuke.kukeboard.article.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kuke.kukeboard.article.service.ArticleService;
import kuke.kukeboard.article.service.request.ArticleCreateRequest;
import kuke.kukeboard.article.service.request.ArticleUpdateRequest;
import kuke.kukeboard.article.service.response.ArticleInfiniteScrollResponse;
import kuke.kukeboard.article.service.response.ArticlePageResponse;
import kuke.kukeboard.article.service.response.ArticleResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    public ArticleResponse create(@RequestBody ArticleCreateRequest request) {
        return articleService.create(request);
    }

    /**
     * Page-number-block pagination. Supports jumping to an arbitrary page
     * and tells the caller the last real page number within the current
     * block of {@code pageLimit} page numbers, so FE can render a
     * "[1][2]...[10][다음]" style page bar without a full COUNT(*).
     */
    @GetMapping
    public ArticlePageResponse readAll(
            @RequestParam Long boardId,
            @RequestParam Long page,
            @RequestParam Long pageSize,
            @RequestParam(defaultValue = "10") Long pageLimit
    ) {
        return articleService.readAll(boardId, page, pageSize, pageLimit);
    }

    /**
     * Cursor(keyset) pagination for infinite scroll. Cost is constant
     * regardless of how deep the caller has already scrolled.
     */
    @GetMapping("/infinite-scroll")
    public ArticleInfiniteScrollResponse readAllInfiniteScroll(
            @RequestParam Long boardId,
            @RequestParam(required = false) Long lastArticleId,
            @RequestParam Long pageSize
    ) {
        return articleService.readAllInfiniteScroll(boardId, lastArticleId, pageSize);
    }

    @GetMapping("/{articleId}")
    public ArticleResponse read(@PathVariable Long articleId) {
        return articleService.read(articleId);
    }

    @PutMapping("/{articleId}")
    public ArticleResponse update(@PathVariable Long articleId, @RequestBody ArticleUpdateRequest request) {
        return articleService.update(articleId, request);
    }

    @DeleteMapping("/{articleId}")
    public void delete(@PathVariable Long articleId) {
        articleService.delete(articleId);
    }
}
