package kuke.kukeboard.article.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kuke.board.common.snowflake.Snowflake;
import kuke.kukeboard.article.entity.Article;
import kuke.kukeboard.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final Snowflake snowflake = new Snowflake();
    private final ArticleRepository articleRepository;

    @Transactional
    public Article create(String title, String content, Long boardId, Long writerId) {
        Article article = Article.create(snowflake.nextId(), title, content, boardId, writerId);
        return articleRepository.save(article);
    }

    public Article read(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: articleId=" + articleId));
    }

    @Transactional
    public Article update(Long articleId, String title, String content) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: articleId=" + articleId));
        article.update(title, content);
        return article;
    }

    @Transactional
    public void delete(Long articleId) {
        articleRepository.deleteById(articleId);
    }
}
