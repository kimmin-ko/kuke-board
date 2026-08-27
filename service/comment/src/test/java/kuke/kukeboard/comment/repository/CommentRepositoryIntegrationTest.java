package kuke.kukeboard.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;

import kuke.kukeboard.comment.entity.Comment;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true",
        "logging.level.org.hibernate.orm.jdbc.bind=trace"
})
@DisplayName("CommentRepository 통합 테스트 (Testcontainers MySQL)")
class CommentRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withInitScript("db/rdb-schema.sql");

    private static final FixtureMonkey FIXTURE_MONKEY = FixtureMonkey.builder()
            .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
            .build();

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
    @DisplayName("루트 댓글을 저장하면 그대로 조회되고, isRoot()는 true다")
    void saveAndFindRootComment() {
        Comment root = commentFixture(1L, 1L, false);

        commentRepository.save(root);

        Comment found = commentRepository.findById(1L).orElseThrow();
        assertThat(found.getCommentId()).isEqualTo(1L);
        assertThat(found.isRoot()).isTrue();
    }

    @Test
    @DisplayName("답글이 있는 루트 댓글은 자식 존재로 판정되고, 답글 자신은 자식이 없다고 판정된다")
    void existsByParentCommentIdAndCommentIdNot_detectsRemainingChildren() {
        Comment root = commentFixture(10L, 10L, false);
        Comment reply = commentFixture(11L, 10L, false);
        commentRepository.saveAll(List.of(root, reply));

        boolean rootHasChildren =
                commentRepository.existsByParentCommentIdAndCommentIdNot(10L, 10L);
        boolean replyHasChildren =
                commentRepository.existsByParentCommentIdAndCommentIdNot(11L, 11L);

        assertThat(rootHasChildren).isTrue();
        assertThat(replyHasChildren).isFalse();
    }

    @Test
    @DisplayName("마지막 남은 답글을 삭제하면 루트 댓글은 더 이상 자식이 없다고 판정된다")
    void existsByParentCommentIdAndCommentIdNot_falseAfterOnlyChildRemoved() {
        Comment root = commentFixture(20L, 20L, true);
        Comment reply = commentFixture(21L, 20L, false);
        commentRepository.saveAll(List.of(root, reply));

        commentRepository.delete(reply);

        boolean rootHasChildren =
                commentRepository.existsByParentCommentIdAndCommentIdNot(20L, 20L);
        assertThat(rootHasChildren).isFalse();
    }
}
