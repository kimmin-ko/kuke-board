package kuke.kukeboard.comment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;

import kuke.kukeboard.comment.AbstractIntegrationTest;
import kuke.kukeboard.comment.entity.Comment;
import kuke.kukeboard.comment.repository.CommentRepository;
import kuke.kukeboard.comment.service.request.CommentCreateRequest;
import kuke.kukeboard.comment.service.response.CommentResponse;

@DisplayName("CommentService 통합 테스트 (Testcontainers MySQL)")
class CommentServiceIntegrationTest extends AbstractIntegrationTest {

    private static final FixtureMonkey FIXTURE_MONKEY = FixtureMonkey.builder()
            .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
            .build();

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
    }

    private Comment commentFixture(long commentId, long parentCommentId, boolean deleted) {
        return FIXTURE_MONKEY.giveMeBuilder(Comment.class)
                .set("commentId", commentId)
                .set("parentCommentId", parentCommentId)
                .set("deleted", deleted)
                .setNotNull("content")
                .setNotNull("articleId")
                .setNotNull("writerId")
                .setNotNull("createdAt")
                .sample();
    }

    @Test
    @DisplayName("parentCommentId 없이 생성하면 자기 자신을 부모로 갖는 루트 댓글이 된다")
    void createRoot() {
        CommentResponse response = commentService.create(
                new CommentCreateRequest("hello", 1L, 1L, null));

        assertThat(response.parentCommentId()).isEqualTo(response.commentId());
        assertThat(commentRepository.findById(response.commentId())).isPresent();
    }

    @Test
    @DisplayName("루트 댓글의 commentId를 parentCommentId로 넘기면 그 아래 답글로 붙는다")
    void createReplyUnderRoot() {
        Comment root = commentFixture(1L, 1L, false);
        commentRepository.save(root);

        CommentResponse reply = commentService.create(
                new CommentCreateRequest("reply", 1L, 2L, 1L));

        assertThat(reply.parentCommentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("답글의 commentId를 parentCommentId로 넘겨도(답글에 답글) 원댓글 아래로 평탄화된다")
    void replyToReplyFlattensOntoRoot() {
        Comment root = commentFixture(1L, 1L, false);
        Comment reply = commentFixture(2L, 1L, false);
        commentRepository.saveAll(List.of(root, reply));

        CommentResponse replyToReply = commentService.create(
                new CommentCreateRequest("re-reply", 1L, 3L, 2L));

        assertThat(replyToReply.parentCommentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("삭제 표시된 댓글을 조회하면 content가 마스킹되어 노출된다")
    void readDeletedCommentMasksContent() {
        Comment deletedRoot = commentFixture(1L, 1L, true);
        commentRepository.save(deletedRoot);

        CommentResponse response = commentService.read(1L);

        assertThat(response.deleted()).isTrue();
        assertThat(response.content()).isEqualTo("삭제된 댓글입니다.");
    }

    @Test
    @DisplayName("자식이 있는 댓글을 삭제하면 물리 삭제되지 않고 삭제 표시만 된다")
    void deleteWithChildrenOnlyMarksDeleted() {
        Comment root = commentFixture(1L, 1L, false);
        Comment reply = commentFixture(2L, 1L, false);
        commentRepository.saveAll(List.of(root, reply));

        commentService.delete(1L);

        Comment found = commentRepository.findById(1L).orElseThrow();
        assertThat(found.getDeleted()).isTrue();
    }

    @Test
    @DisplayName("자식이 없는 댓글을 삭제하면 즉시 물리 삭제된다")
    void deleteLeafHardDeletesImmediately() {
        Comment root = commentFixture(1L, 1L, false);
        Comment reply = commentFixture(2L, 1L, false);
        commentRepository.saveAll(List.of(root, reply));

        commentService.delete(2L);

        assertThat(commentRepository.findById(2L)).isEmpty();
    }

    @Test
    @DisplayName("마지막 남은 답글이 삭제되면, 이미 삭제 표시된 부모도 연쇄적으로 물리 삭제된다")
    void deletingLastChildCascadesDeletedParentRemoval() {
        Comment root = commentFixture(1L, 1L, true);
        Comment reply = commentFixture(2L, 1L, false);
        commentRepository.saveAll(List.of(root, reply));

        commentService.delete(2L);

        assertThat(commentRepository.findById(1L)).isEmpty();
        assertThat(commentRepository.findById(2L)).isEmpty();
    }
}
