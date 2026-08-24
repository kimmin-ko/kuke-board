package kuke.kukeboard.article.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kuke.board.common.snowflake.Snowflake;
import kuke.kukeboard.article.entity.Article;
import kuke.kukeboard.article.repository.ArticleRepository;
import kuke.kukeboard.article.service.request.ArticleCreateRequest;
import kuke.kukeboard.article.service.request.ArticleUpdateRequest;
import kuke.kukeboard.article.service.response.ArticleInfiniteScrollResponse;
import kuke.kukeboard.article.service.response.ArticlePageResponse;
import kuke.kukeboard.article.service.response.ArticleResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final Snowflake snowflake = new Snowflake();
    private final ArticleRepository articleRepository;

    /**
     * Page-number-block pagination: instead of a full COUNT(*), bounds the
     * covering-index scan to exactly what's needed to know the last real
     * page in the current block of {@code pageLimit} page numbers (and
     * whether a next block exists) -- cost depends on block position, not
     * on total table size.
     */
    public ArticlePageResponse readAll(Long boardId, Long page, Long pageSize, Long pageLimit) {
        long blockIndex = (page - 1) / pageLimit;
        long limit = (blockIndex + 1) * pageSize * pageLimit + 1;

        List<Long> boundedIds = articleRepository.findIds(boardId, PageRequest.of(0, (int) limit));
        long available = boundedIds.size();

        boolean hasNext = available == limit;
        long lastPage = hasNext ? (blockIndex + 1) * pageLimit : ceilDiv(available, pageSize);

        long start = (page - 1) * pageSize;
        List<Long> pageIds = start >= available
                ? List.of()
                : boundedIds.subList((int) start, (int) Math.min(start + pageSize, available));

        List<ArticleResponse> articles = pageIds.isEmpty()
                ? List.of()
                : articleRepository.findAllByIds(pageIds).stream().map(ArticleResponse::from).toList();

        return new ArticlePageResponse(articles, page, pageSize, pageLimit, lastPage, hasNext);
    }

    private static long ceilDiv(long dividend, long divisor) {
        return (dividend + divisor - 1) / divisor;
    }

    public ArticleInfiniteScrollResponse readAllInfiniteScroll(Long boardId, Long lastArticleId, Long pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize.intValue() + 1);
        List<Article> found = articleRepository.findAllByCursor(boardId, lastArticleId, pageable);

        boolean hasNext = found.size() > pageSize;
        List<Article> pageArticles = hasNext ? found.subList(0, pageSize.intValue()) : found;

        List<ArticleResponse> articles = pageArticles.stream().map(ArticleResponse::from).toList();
        Long nextCursor = articles.isEmpty() ? null : articles.getLast().articleId();

        return new ArticleInfiniteScrollResponse(articles, nextCursor, hasNext);
    }

    @Transactional
    public ArticleResponse create(ArticleCreateRequest request) {
        Article article = Article.create(
                snowflake.nextId(),
                request.title(),
                request.content(),
                request.boardId(),
                request.writerId()
        );
        return ArticleResponse.from(articleRepository.save(article));
    }

    public ArticleResponse read(Long articleId) {
        return ArticleResponse.from(findOrThrow(articleId));
    }

    @Transactional
    public ArticleResponse update(Long articleId, ArticleUpdateRequest request) {
        Article article = findOrThrow(articleId);
        article.update(request.title(), request.content());
        return ArticleResponse.from(article);
    }

    @Transactional
    public void delete(Long articleId) {
        articleRepository.deleteById(articleId);
    }

    private Article findOrThrow(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: articleId=" + articleId));
    }
}
