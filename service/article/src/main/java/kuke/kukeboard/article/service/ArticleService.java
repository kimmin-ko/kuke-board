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
import kuke.kukeboard.article.service.response.ArticleResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final Snowflake snowflake = new Snowflake();
    private final ArticleRepository articleRepository;

    public List<ArticleResponse> readAll(Long boardId, Long page, Long pageSize) {
        Pageable pageable = PageRequest.of((int) (page - 1), pageSize.intValue());
        List<Long> articleIds = articleRepository.findIds(boardId, pageable);
        if (articleIds.isEmpty()) {
            return List.of();
        }
        return articleRepository.findAllByIds(articleIds)
                .stream()
                .map(ArticleResponse::from)
                .toList();
    }

    public List<ArticleResponse> readAllInfiniteScroll(Long boardId, Long lastArticleId, Long pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize.intValue());
        return articleRepository.findAllByCursor(boardId, lastArticleId, pageable)
                .stream()
                .map(ArticleResponse::from)
                .toList();
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
