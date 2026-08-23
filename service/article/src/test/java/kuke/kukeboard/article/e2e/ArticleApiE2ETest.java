package kuke.kukeboard.article.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import kuke.kukeboard.article.repository.ArticleRepository;
import kuke.kukeboard.article.service.request.ArticleCreateRequest;
import kuke.kukeboard.article.service.request.ArticleUpdateRequest;
import kuke.kukeboard.article.service.response.ArticleResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArticleApiE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private ArticleRepository articleRepository;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @AfterEach
    void tearDown() {
        articleRepository.deleteAll();
    }

    @Test
    void createReadUpdateDelete() {
        ArticleResponse created = client.post()
                .uri("/v1/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ArticleCreateRequest("title", "content", 1L, 1L))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ArticleResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.title()).isEqualTo("title");
        assertThat(created.content()).isEqualTo("content");
        assertThat(created.boardId()).isEqualTo(1L);
        assertThat(created.writerId()).isEqualTo(1L);
        Long articleId = created.articleId();

        ArticleResponse read = client.get()
                .uri("/v1/articles/{articleId}", articleId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ArticleResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(read).isNotNull();
        assertThat(read.title()).isEqualTo("title");

        ArticleResponse updated = client.put()
                .uri("/v1/articles/{articleId}", articleId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ArticleUpdateRequest("title2", "content2"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ArticleResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.title()).isEqualTo("title2");
        assertThat(updated.content()).isEqualTo("content2");

        client.delete()
                .uri("/v1/articles/{articleId}", articleId)
                .exchange()
                .expectStatus().isOk();

        assertThat(articleRepository.findById(articleId)).isEmpty();
    }
}
