package kuke.kukeboard.article.service.response;

import java.time.LocalDateTime;

import kuke.kukeboard.article.entity.Article;

public record ArticleResponse(
        Long articleId,
        String title,
        String content,
        Long boardId,
        Long writerId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getArticleId(),
                article.getTitle(),
                article.getContent(),
                article.getBoardId(),
                article.getWriterId(),
                article.getCreatedAt(),
                article.getModifiedAt()
        );
    }
}
