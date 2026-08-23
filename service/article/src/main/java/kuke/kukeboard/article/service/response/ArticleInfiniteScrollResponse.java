package kuke.kukeboard.article.service.response;

import java.util.List;

public record ArticleInfiniteScrollResponse(
        List<ArticleResponse> articles,
        Long nextCursor,
        boolean hasNext
) {
}
