package kuke.kukeboard.comment.service.request;

public record CommentCreateRequest(
        String content,
        Long articleId,
        Long writerId,
        Long parentCommentId
) {
}
