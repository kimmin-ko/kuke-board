package kuke.kukeboard.comment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kuke.board.common.snowflake.Snowflake;
import kuke.kukeboard.comment.entity.Comment;
import kuke.kukeboard.comment.repository.CommentRepository;
import kuke.kukeboard.comment.service.request.CommentCreateRequest;
import kuke.kukeboard.comment.service.response.CommentResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final Snowflake snowflake = new Snowflake();
    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponse create(CommentCreateRequest request) {
        Long commentId = snowflake.nextId();

        Comment comment = request.parentCommentId() == null
                ? Comment.createRoot(commentId, request.content(), request.articleId(), request.writerId())
                : Comment.createReply(commentId, request.content(), request.articleId(), resolveRootId(request.parentCommentId()), request.writerId());

        return CommentResponse.from(commentRepository.save(comment));
    }

    public CommentResponse read(Long commentId) {
        return CommentResponse.from(findOrThrow(commentId));
    }

    @Transactional
    public void delete(Long commentId) {
        Comment comment = findOrThrow(commentId);

        if (hasChildren(comment)) {
            comment.delete();
            return;
        }

        commentRepository.delete(comment);
        if (!comment.isRoot()) {
            deleteParentIfOrphanedAndDeleted(comment.getParentCommentId());
        }
    }

    /**
     * Max 2 depth: a reply-to-a-reply flattens to a reply on the original
     * root, so every comment ever has at most one level of children.
     */
    private Long resolveRootId(Long parentCommentId) {
        Comment parent = findOrThrow(parentCommentId);
        return parent.isRoot() ? parent.getCommentId() : parent.getParentCommentId();
    }

    private void deleteParentIfOrphanedAndDeleted(Long parentCommentId) {
        commentRepository.findById(parentCommentId).ifPresent(parent -> {
            if (Boolean.TRUE.equals(parent.getDeleted()) && !hasChildren(parent)) {
                commentRepository.delete(parent);
            }
        });
    }

    private boolean hasChildren(Comment comment) {
        return commentRepository.existsByParentCommentIdAndCommentIdNot(comment.getCommentId(), comment.getCommentId());
    }

    private Comment findOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: commentId=" + commentId));
    }
}
