package kuke.kukeboard.comment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @Column(name = "comment_id")
    private Long commentId;

    @Column(nullable = false, length = 3000)
    private String content;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "parent_comment_id", nullable = false)
    private Long parentCommentId;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @Column(nullable = false)
    private Boolean deleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Comment createRoot(Long commentId, String content, Long articleId, Long writerId) {
        return create(commentId, content, articleId, commentId, writerId);
    }

    public static Comment createReply(Long commentId, String content, Long articleId, Long parentCommentId, Long writerId) {
        return create(commentId, content, articleId, parentCommentId, writerId);
    }

    private static Comment create(Long commentId, String content, Long articleId, Long parentCommentId, Long writerId) {
        Comment comment = new Comment();
        comment.commentId = commentId;
        comment.content = content;
        comment.articleId = articleId;
        comment.parentCommentId = parentCommentId;
        comment.writerId = writerId;
        comment.deleted = false;
        comment.createdAt = LocalDateTime.now();
        return comment;
    }

    public boolean isRoot() {
        return commentId.equals(parentCommentId);
    }

    public void delete() {
        this.deleted = true;
    }
}
