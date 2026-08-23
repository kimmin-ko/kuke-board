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

    public ArticlePageResponse readAll(Long boardId, Long page, Long pageSize) {
        Pageable pageable = PageRequest.of((int) (page - 1), pageSize.intValue());
        List<Long> articleIds = articleRepository.findIds(boardId, pageable);

        List<ArticleResponse> articles = articleIds.isEmpty()
                ? List.of()
                : articleRepository.findAllByIds(articleIds).stream().map(ArticleResponse::from).toList();

        boolean hasNext = !articles.isEmpty() && !articleRepository.findAllByCursor(
                boardId, articles.get(articles.size() - 1).articleId(), PageRequest.of(0, 1)
        ).isEmpty();

        return new ArticlePageResponse(articles, page, pageSize, hasNext);
    }

    public ArticleInfiniteScrollResponse readAllInfiniteScroll(Long boardId, Long lastArticleId, Long pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize.intValue() + 1);
        List<Article> found = articleRepository.findAllByCursor(boardId, lastArticleId, pageable);

        boolean hasNext = found.size() > pageSize;
        List<Article> pageArticles = hasNext ? found.subList(0, pageSize.intValue()) : found;

        List<ArticleResponse> articles = pageArticles.stream().map(ArticleResponse::from).toList();
        Long nextCursor = articles.isEmpty() ? null : articles.get(articles.size() - 1).articleId();

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
