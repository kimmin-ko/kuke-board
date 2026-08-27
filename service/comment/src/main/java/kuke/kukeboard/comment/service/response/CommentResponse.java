package kuke.kukeboard.comment.service.response;

import java.time.LocalDateTime;

import kuke.kukeboard.comment.entity.Comment;

public record CommentResponse(
        Long commentId,
        String content,
        Long articleId,
        Long parentCommentId,
        Long writerId,
        boolean deleted,
        LocalDateTime createdAt
) {
    private static final String DELETED_CONTENT = "삭제된 댓글입니다.";

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getCommentId(),
                Boolean.TRUE.equals(comment.getDeleted()) ? DELETED_CONTENT : comment.getContent(),
                comment.getArticleId(),
                comment.getParentCommentId(),
                comment.getWriterId(),
                comment.getDeleted(),
                comment.getCreatedAt()
        );
    }
}
