package kuke.board.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageLimitCalculatorTest {

    @Test
    void calculateLimit() {
        assertThat(PageLimitCalculator.calculateLimit(7, 30, 10)).isEqualTo(301);
        assertThat(PageLimitCalculator.calculateLimit(12, 30, 10)).isEqualTo(601);
        assertThat(PageLimitCalculator.calculateLimit(1, 30, 10)).isEqualTo(301);
        assertThat(PageLimitCalculator.calculateLimit(10, 30, 10)).isEqualTo(301);
    }

    @Test
    void calculateLastPageWhenBlockIsFull() {
        long limit = PageLimitCalculator.calculateLimit(7, 30, 10);

        long lastPage = PageLimitCalculator.calculateLastPage(7, 30, 10, limit);

        assertThat(lastPage).isEqualTo(10);
    }

    @Test
    void calculateLastPageWhenDataRunsOutMidBlock() {
        // block [1..10], page size 30 -> only 125 rows exist (page 5 is the last with data)
        long available = 125;

        long lastPage = PageLimitCalculator.calculateLastPage(7, 30, 10, available);

        assertThat(lastPage).isEqualTo(5);
    }

    @Test
    void calculateLastPageWhenNoData() {
        long lastPage = PageLimitCalculator.calculateLastPage(1, 30, 10, 0);

        assertThat(lastPage).isEqualTo(0);
    }
}
