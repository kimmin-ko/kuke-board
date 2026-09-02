package kuke.kukeboard.comment.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kuke.board.common.pagination.PageLimitCalculator;
import kuke.board.common.pagination.PageResponse;
import kuke.board.common.snowflake.Snowflake;
import kuke.kukeboard.comment.entity.Comment;
import kuke.kukeboard.comment.repository.CommentRepository;
import kuke.kukeboard.comment.service.request.CommentCreateRequest;
import kuke.kukeboard.comment.service.response.CommentCursorResponse;
import kuke.kukeboard.comment.service.response.CommentResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final Snowflake snowflake = new Snowflake();
    private final CommentRepository commentRepository;

    /**
     * Page-number-block pagination, root-first then replies oldest-first
     * within each root. Same bounded-scan trick as article: no full
     * COUNT(*), cost depends on block position, not table size.
     */
    public PageResponse<CommentResponse> readAll(Long articleId, Long page, Long pageSize, Long pageLimit) {
        long limit = PageLimitCalculator.calculateLimit(page, pageSize, pageLimit);

        List<Long> boundedIds = commentRepository.findIds(articleId, PageRequest.of(0, (int) limit));
        long available = boundedIds.size();

        boolean hasNext = available == limit;
        long lastPage = PageLimitCalculator.calculateLastPage(page, pageSize, pageLimit, available);
        List<Long> pageIds = PageLimitCalculator.sliceForPage(boundedIds, page, pageSize);

        List<CommentResponse> comments = pageIds.isEmpty()
                ? List.of()
                : commentRepository.findAllByIds(pageIds).stream().map(CommentResponse::from).toList();

        return new PageResponse<>(comments, page, pageSize, pageLimit, lastPage, hasNext);
    }

    /**
     * Cursor(keyset) pagination for infinite scroll. The sort key is
     * composite (parentCommentId, commentId), so both must be carried in
     * the cursor to resume a seek at the right spot.
     */
    public CommentCursorResponse<CommentResponse> readAllInfiniteScroll(
            Long articleId, Long lastParentCommentId, Long lastCommentId, Long pageSize
    ) {
        Pageable pageable = PageRequest.of(0, pageSize.intValue() + 1);
        List<Comment> found = commentRepository.findAllByCursor(articleId, lastParentCommentId, lastCommentId, pageable);

        boolean hasNext = found.size() > pageSize;
        List<Comment> pageComments = hasNext ? found.subList(0, pageSize.intValue()) : found;

        List<CommentResponse> comments = pageComments.stream().map(CommentResponse::from).toList();
        Comment lastComment = pageComments.isEmpty() ? null : pageComments.getLast();
        Long nextParentCommentId = lastComment == null ? null : lastComment.getParentCommentId();
        Long nextCommentId = lastComment == null ? null : lastComment.getCommentId();

        return new CommentCursorResponse<>(comments, nextParentCommentId, nextCommentId, hasNext);
    }

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
