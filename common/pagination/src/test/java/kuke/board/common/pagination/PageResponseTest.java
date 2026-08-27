package kuke.board.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PageResponse 단위 테스트")
class PageResponseTest {

    @Test
    @DisplayName("생성자로 넘긴 모든 필드를 접근자로 그대로 꺼낼 수 있다")
    void exposesAllFieldsThroughAccessors() {
        PageResponse<String> response = new PageResponse<>(List.of("a", "b"), 1, 10, 10, 5, true);

        assertThat(response.data()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.pageLimit()).isEqualTo(10);
        assertThat(response.lastPage()).isEqualTo(5);
        assertThat(response.hasNext()).isTrue();
    }
}
