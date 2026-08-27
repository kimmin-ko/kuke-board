package kuke.kukeboard.article;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ArticleApplication 컨텍스트/부팅 테스트")
class ArticleApplicationTests extends AbstractIntegrationTest {

    @Test
    @DisplayName("Spring 컨텍스트가 정상적으로 로드된다")
    void contextLoads() {
    }

    /**
     * Calls the real main(String[]) entry point (not just the
     * SpringApplication bootstrapping @SpringBootTest does under the hood),
     * against the shared Testcontainers MySQL so it doesn't depend on a
     * local dev database being up. --server.port=0 picks a free port so it
     * doesn't collide with anything else in the suite.
     */
    @Test
    @DisplayName("main()으로 애플리케이션이 정상적으로 기동된다")
    void main() {
        assertThatCode(() -> ArticleApplication.main(new String[] {
                "--server.port=0",
                "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "--spring.datasource.username=" + MYSQL.getUsername(),
                "--spring.datasource.password=" + MYSQL.getPassword()
        })).doesNotThrowAnyException();
    }

}
