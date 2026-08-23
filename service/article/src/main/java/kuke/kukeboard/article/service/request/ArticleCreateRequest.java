package kuke.kukeboard.article.service.request;

public record ArticleCreateRequest(
        String title,
        String content,
        Long boardId,
        Long writerId
) {
}
