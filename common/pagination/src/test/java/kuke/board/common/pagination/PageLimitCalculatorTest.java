package kuke.board.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PageLimitCalculator 단위 테스트")
class PageLimitCalculatorTest {

    @Test
    @DisplayName("주어진 page/pageSize/pageLimit로부터 bounded scan에 필요한 limit을 계산한다")
    void calculateLimit() {
        assertThat(PageLimitCalculator.calculateLimit(7, 30, 10)).isEqualTo(301);
        assertThat(PageLimitCalculator.calculateLimit(12, 30, 10)).isEqualTo(601);
        assertThat(PageLimitCalculator.calculateLimit(1, 30, 10)).isEqualTo(301);
        assertThat(PageLimitCalculator.calculateLimit(10, 30, 10)).isEqualTo(301);
    }

    @Test
    @DisplayName("블록이 꽉 찼으면(available == limit) lastPage는 블록의 마지막 페이지 번호다")
    void calculateLastPageWhenBlockIsFull() {
        long limit = PageLimitCalculator.calculateLimit(7, 30, 10);

        long lastPage = PageLimitCalculator.calculateLastPage(7, 30, 10, limit);

        assertThat(lastPage).isEqualTo(10);
    }

    @Test
    @DisplayName("블록 중간에 데이터가 끊기면 lastPage는 실제 남은 건수로 계산된다")
    void calculateLastPageWhenDataRunsOutMidBlock() {
        // block [1..10], page size 30 -> only 125 rows exist (page 5 is the last with data)
        long available = 125;

        long lastPage = PageLimitCalculator.calculateLastPage(7, 30, 10, available);

        assertThat(lastPage).isEqualTo(5);
    }

    @Test
    @DisplayName("데이터가 하나도 없으면 lastPage는 0이다")
    void calculateLastPageWhenNoData() {
        long lastPage = PageLimitCalculator.calculateLastPage(1, 30, 10, 0);

        assertThat(lastPage).isZero();
    }

    @Test
    @DisplayName("bounded 리스트에서 요청한 페이지 구간만 정확히 잘라낸다")
    void sliceForPageReturnsThePageWindow() {
        List<Integer> bounded = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        assertThat(PageLimitCalculator.sliceForPage(bounded, 1, 3)).containsExactly(0, 1, 2);
        assertThat(PageLimitCalculator.sliceForPage(bounded, 2, 3)).containsExactly(3, 4, 5);
        assertThat(PageLimitCalculator.sliceForPage(bounded, 4, 3)).containsExactly(9);
    }

    @Test
    @DisplayName("요청한 페이지가 데이터 끝을 넘어가면 빈 리스트를 반환한다")
    void sliceForPageReturnsEmptyPastTheEnd() {
        List<Integer> bounded = List.of(0, 1, 2);

        assertThat(PageLimitCalculator.sliceForPage(bounded, 2, 3)).isEmpty();
    }
}
