package kuke.kukeboard.comment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Comment 엔티티 단위 테스트 (Spring 컨텍스트 없음)")
class CommentTest {

    @Test
    @DisplayName("createRoot로 만든 댓글은 자기 자신을 부모로 가지며, isRoot()는 true다")
    void createRootIsRoot() {
        Comment root = Comment.createRoot(1L, "content", 10L, 100L);

        assertThat(root.getParentCommentId()).isEqualTo(root.getCommentId());
        assertThat(root.isRoot()).isTrue();
    }

    @Test
    @DisplayName("createReply로 만든 댓글은 지정한 부모를 가지며, isRoot()는 false다")
    void createReplyIsNotRoot() {
        Comment reply = Comment.createReply(2L, "content", 10L, 1L, 100L);

        assertThat(reply.getParentCommentId()).isEqualTo(1L);
        assertThat(reply.isRoot()).isFalse();
    }

    @Test
    @DisplayName("delete()를 호출하면 deleted 플래그가 true가 된다")
    void deleteMarksDeleted() {
        Comment comment = Comment.createRoot(1L, "content", 10L, 100L);

        comment.delete();

        assertThat(comment.getDeleted()).isTrue();
    }
}
