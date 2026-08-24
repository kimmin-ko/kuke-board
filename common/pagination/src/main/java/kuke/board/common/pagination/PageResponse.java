package kuke.board.common.pagination;

import java.util.List;

/**
 * Page-number-block pagination response. {@code lastPage} is the last real
 * page number within the current block of {@code pageLimit} page numbers,
 * and {@code hasNext} indicates whether a next block exists -- both derived
 * from a bounded scan (see PageLimitCalculator), never a full COUNT(*).
 */
public record PageResponse<T>(
        List<T> data,
        long page,
        long pageSize,
        long pageLimit,
        long lastPage,
        boolean hasNext
) {
}
