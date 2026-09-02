package kuke.kukeboard.comment.service.response;

import java.util.List;

/**
 * Cursor(keyset) pagination response for infinite scroll. Comments sort by
 * a composite key (parentCommentId, commentId) -- root-first, then replies
 * oldest-first within each root -- so unlike a single-key cursor, resuming
 * from the last row needs both {@code nextParentCommentId} and
 * {@code nextCommentId}.
 */
public record CommentCursorResponse<T>(
        List<T> data,
        Long nextParentCommentId,
        Long nextCommentId,
        boolean hasNext
) {
}
