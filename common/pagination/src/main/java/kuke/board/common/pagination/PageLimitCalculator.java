package kuke.board.common.pagination;

/**
 * Bounds a covering-index scan to just enough rows to answer "what's the
 * last real page in the current block of {@code pageLimit} page numbers,
 * and is there a next block?" -- without a full COUNT(*).
 */
public class PageLimitCalculator {

    private PageLimitCalculator() {
    }

    /**
     * Number of rows to fetch (in page order) so the caller can determine
     * both the current page's data and the current block's last page.
     */
    public static long calculateLimit(long page, long pageSize, long pageLimit) {
        long blockIndex = (page - 1) / pageLimit;
        return (blockIndex + 1) * pageSize * pageLimit + 1;
    }

    /**
     * @param available number of rows actually returned by a fetch bounded
     *                  to {@link #calculateLimit}
     */
    public static long calculateLastPage(long page, long pageSize, long pageLimit, long available) {
        long limit = calculateLimit(page, pageSize, pageLimit);
        if (available == limit) {
            long blockIndex = (page - 1) / pageLimit;
            return (blockIndex + 1) * pageLimit;
        }
        return (available + pageSize - 1) / pageSize;
    }
}
