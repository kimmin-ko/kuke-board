package kuke.kukeboard.comment;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton container pattern: the container is started once (via the
 * static initializer, not the {@code @Testcontainers} JUnit lifecycle) and
 * shared by every integration test class in this module for the whole test
 * run, instead of each class paying its own ~5-10s startup cost. Testcontainers'
 * own Ryuk reaper container tears it down when the JVM exits.
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
