package kuke.kukeboard.comment.init;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import kuke.board.common.snowflake.Snowflake;

/**
 * Manual bulk data seeding for local performance/sharding testing.
 * Not part of the regular test suite — run explicitly via IDE or
 * {@code ./gradlew test --tests "*CommentDataInitializer*"} after removing @Disabled.
 */
@SpringBootTest(properties = {
        "logging.level.root=WARN",
        "spring.main.banner-mode=off",
        "spring.datasource.hikari.maximum-pool-size=20"
})
class CommentDataInitializer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final long ARTICLE_ID_START = 1L;
    private static final long ARTICLE_COUNT = 100_000L;
    private static final int COMMENTS_PER_ARTICLE = 120;
    private static final int ROOTS_PER_ARTICLE = 30;
    private static final int ARTICLES_PER_BATCH = 20;
    private static final long EXECUTE_COUNT = ARTICLE_COUNT / ARTICLES_PER_BATCH;
    private static final int THREAD_COUNT = 20;

    private final Snowflake snowflake = new Snowflake();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Disabled("Seeds 12M rows (root + depth-2 replies) into the comment table, spread evenly over 100K articles — run manually, not part of CI")
    @Test
    void initialize() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch((int) EXECUTE_COUNT);

        for (long batchIndex = 0; batchIndex < EXECUTE_COUNT; batchIndex++) {
            long articleStart = ARTICLE_ID_START + batchIndex * ARTICLES_PER_BATCH;
            executorService.submit(() -> {
                try {
                    insertBulk(articleStart);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
    }

    private void insertBulk(long articleStart) {
        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        StringBuilder sql = new StringBuilder(
                "insert into comment (comment_id, content, article_id, parent_comment_id, writer_id, deleted, created_at) values "
        );

        boolean first = true;
        for (int a = 0; a < ARTICLES_PER_BATCH; a++) {
            long articleId = articleStart + a;
            long[] rootCommentIds = new long[ROOTS_PER_ARTICLE];

            for (int r = 0; r < ROOTS_PER_ARTICLE; r++) {
                long commentId = snowflake.nextId();
                rootCommentIds[r] = commentId;
                first = appendRow(sql, first, commentId, articleId, commentId, now);
            }

            int replyCount = COMMENTS_PER_ARTICLE - ROOTS_PER_ARTICLE;
            for (int i = 0; i < replyCount; i++) {
                long commentId = snowflake.nextId();
                long parentCommentId = rootCommentIds[i % ROOTS_PER_ARTICLE];
                first = appendRow(sql, first, commentId, articleId, parentCommentId, now);
            }
        }

        jdbcTemplate.execute(sql.toString());
    }

    private boolean appendRow(StringBuilder sql, boolean first, long commentId, long articleId, long parentCommentId, String now) {
        if (!first) {
            sql.append(",");
        }
        sql.append(String.format(
                "(%d, 'content %d', %d, %d, %d, false, '%s')",
                commentId, commentId, articleId, parentCommentId, 1L, now
        ));
        return false;
    }
}
