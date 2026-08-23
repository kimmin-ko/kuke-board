package kuke.kukeboard.article.init;

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
 * {@code ./gradlew test --tests "*ArticleDataInitializer*"} after removing @Disabled.
 */
@SpringBootTest(properties = {
        "logging.level.root=WARN",
        "spring.main.banner-mode=off",
        "spring.datasource.hikari.maximum-pool-size=20"
})
class ArticleDataInitializer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int BULK_INSERT_SIZE = 2000;
    private static final long FIXED_BOARD_ID = 1L;
    private static final long TOTAL_ROW_COUNT = 12_000_000L;
    private static final long EXECUTE_COUNT = TOTAL_ROW_COUNT / BULK_INSERT_SIZE;
    private static final int THREAD_COUNT = 20;

    private final Snowflake snowflake = new Snowflake();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Disabled("Seeds 12M rows into the article table — run manually, not part of CI")
    @Test
    void initialize() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch((int) EXECUTE_COUNT);

        for (long i = 0; i < EXECUTE_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    insertBulk(FIXED_BOARD_ID);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
    }

    private void insertBulk(long boardId) {
        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        StringBuilder sql = new StringBuilder(
                "insert into article (article_id, title, content, board_id, writer_id, created_at, modified_at) values "
        );

        for (int i = 0; i < BULK_INSERT_SIZE; i++) {
            long articleId = snowflake.nextId();
            sql.append(String.format(
                    "(%d, 'title %d', 'content %d', %d, %d, '%s', '%s')",
                    articleId, articleId, articleId, boardId, 1L, now, now
            ));
            if (i < BULK_INSERT_SIZE - 1) {
                sql.append(",");
            }
        }

        jdbcTemplate.execute(sql.toString());
    }
}
