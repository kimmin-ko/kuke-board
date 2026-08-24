package kuke.board.common.pagination;

import java.util.List;

/**
 * Cursor(keyset) pagination response for infinite scroll. {@code nextCursor}
 * is the value to pass back in as the caller's next request cursor.
 */
public record CursorResponse<T>(
        List<T> data,
        Long nextCursor,
        boolean hasNext
) {
}
