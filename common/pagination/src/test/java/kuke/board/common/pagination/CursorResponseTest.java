package kuke.board.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CursorResponse 단위 테스트")
class CursorResponseTest {

    @Test
    @DisplayName("생성자로 넘긴 모든 필드를 접근자로 그대로 꺼낼 수 있다")
    void exposesAllFieldsThroughAccessors() {
        CursorResponse<String> response = new CursorResponse<>(List.of("a", "b"), 2L, true);

        assertThat(response.data()).containsExactly("a", "b");
        assertThat(response.nextCursor()).isEqualTo(2L);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("더 이상 데이터가 없으면 nextCursor는 null일 수 있다")
    void nextCursorCanBeNullWhenThereIsNoMoreData() {
        CursorResponse<String> response = new CursorResponse<>(List.of(), null, false);

        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasNext()).isFalse();
    }
}
