package kuke.kukeboard.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;

import kuke.board.common.pagination.CursorResponse;
import kuke.board.common.pagination.PageResponse;
import kuke.kukeboard.article.AbstractIntegrationTest;
import kuke.kukeboard.article.entity.Article;
import kuke.kukeboard.article.repository.ArticleRepository;
import kuke.kukeboard.article.service.request.ArticleCreateRequest;
import kuke.kukeboard.article.service.request.ArticleUpdateRequest;
import kuke.kukeboard.article.service.response.ArticleResponse;

@DisplayName("ArticleService 통합 테스트 (Testcontainers MySQL)")
class ArticleServiceIntegrationTest extends AbstractIntegrationTest {

    private static final FixtureMonkey FIXTURE_MONKEY = FixtureMonkey.builder()
            .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
            .build();

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @AfterEach
    void tearDown() {
        articleRepository.deleteAll();
    }

    private Article articleFixture(long articleId, long boardId) {
        return FIXTURE_MONKEY.giveMeBuilder(Article.class)
                .set("articleId", articleId)
                .set("boardId", boardId)
                .set("title", "title" + articleId)
                .set("content", "content" + articleId)
                .setNotNull("writerId")
                .setNotNull("createdAt")
                .setNotNull("modifiedAt")
                .sample();
    }

    @Test
    @DisplayName("게시글을 생성하면 그대로 조회된다")
    void create() {
        ArticleResponse response = articleService.create(
                new ArticleCreateRequest("title", "content", 1L, 1L));

        assertThat(response.title()).isEqualTo("title");
        assertThat(articleRepository.findById(response.articleId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 articleId를 조회하면 IllegalArgumentException이 발생한다")
    void readNonExistentArticleThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> articleService.read(999L))
                .withMessageContaining("999");
    }

    @Test
    @DisplayName("게시글을 수정하면 title/content가 바뀐다")
    void update() {
        articleRepository.save(articleFixture(1L, 1L));

        ArticleResponse response = articleService.update(1L, new ArticleUpdateRequest("new title", "new content"));

        assertThat(response.title()).isEqualTo("new title");
        assertThat(response.content()).isEqualTo("new content");
    }

    @Test
    @DisplayName("게시글을 삭제하면 더 이상 조회되지 않는다")
    void delete() {
        articleRepository.save(articleFixture(1L, 1L));

        articleService.delete(1L);

        assertThat(articleRepository.findById(1L)).isEmpty();
    }

    @Test
    @DisplayName("오프셋 조회: 블록이 꽉 차면 hasNext는 true이고, 요청한 페이지 데이터가 채워진다")
    void readAllWhenBlockIsFullHasNextIsTrue() {
        // pageSize=2, pageLimit=2 -> block0 limit = 1*2*2+1 = 5
        for (long id = 1; id <= 6; id++) {
            articleRepository.save(articleFixture(id, 1L));
        }

        PageResponse<ArticleResponse> response = articleService.readAll(1L, 1L, 2L, 2L);

        assertThat(response.data()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.lastPage()).isEqualTo(2);
    }

    @Test
    @DisplayName("오프셋 조회: 블록 안에서 데이터가 끊기면 hasNext는 false이고, lastPage는 실제 개수로 계산된다")
    void readAllWhenDataRunsOutHasNextIsFalse() {
        for (long id = 1; id <= 3; id++) {
            articleRepository.save(articleFixture(id, 1L));
        }

        PageResponse<ArticleResponse> response = articleService.readAll(1L, 1L, 2L, 2L);

        assertThat(response.data()).hasSize(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.lastPage()).isEqualTo(2);
    }

    @Test
    @DisplayName("오프셋 조회: 해당 게시판에 글이 없으면 빈 목록을 반환한다")
    void readAllWhenNoArticlesReturnsEmptyList() {
        PageResponse<ArticleResponse> response = articleService.readAll(999L, 1L, 2L, 2L);

        assertThat(response.data()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.lastPage()).isZero();
    }

    @Test
    @DisplayName("커서 조회: 남은 데이터가 pageSize보다 많으면 hasNext는 true이고, 마지막 글이 nextCursor가 된다")
    void readAllInfiniteScrollHasNextTrueWhenMoreDataRemains() {
        for (long id = 1; id <= 3; id++) {
            articleRepository.save(articleFixture(id, 1L));
        }

        CursorResponse<ArticleResponse> response = articleService.readAllInfiniteScroll(1L, null, 2L);

        assertThat(response.data()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(response.data().get(1).articleId());
    }

    @Test
    @DisplayName("커서 조회: 남은 데이터가 pageSize 이하이면 hasNext는 false이고, 있는 만큼만 반환한다")
    void readAllInfiniteScrollHasNextFalseWhenDataFitsInOnePage() {
        articleRepository.save(articleFixture(1L, 1L));

        CursorResponse<ArticleResponse> response = articleService.readAllInfiniteScroll(1L, null, 2L);

        assertThat(response.data()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("커서 조회: 결과가 비어있으면 nextCursor는 null이다")
    void readAllInfiniteScrollReturnsNullCursorWhenEmpty() {
        CursorResponse<ArticleResponse> response = articleService.readAllInfiniteScroll(999L, null, 2L);

        assertThat(response.data()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasNext()).isFalse();
    }
}
