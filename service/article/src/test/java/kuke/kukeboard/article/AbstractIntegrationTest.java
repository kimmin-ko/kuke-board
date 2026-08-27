package kuke.kukeboard.article;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton container pattern: the container is started once (via the
 * static initializer, not the {@code @Testcontainers} JUnit lifecycle) and
 * shared by every integration test class in this module for the whole test
 * run. Using an isolated throwaway container instead of the shared local
 * dev database also avoids ever touching the 12M-row dataset seeded there.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withInitScript("db/rdb-schema.sql");

    static {
        MYSQL.start();
    }
}
