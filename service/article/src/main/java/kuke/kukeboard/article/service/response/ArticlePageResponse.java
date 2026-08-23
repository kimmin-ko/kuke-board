package kuke.kukeboard.article.service.response;

import java.util.List;

public record ArticlePageResponse(
        List<ArticleResponse> articles,
        long page,
        long pageSize,
        boolean hasNext
) {
}
