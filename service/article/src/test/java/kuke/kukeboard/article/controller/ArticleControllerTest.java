package kuke.kukeboard.article.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import kuke.board.common.pagination.CursorResponse;
import kuke.board.common.pagination.PageResponse;
import kuke.kukeboard.article.service.ArticleService;
import kuke.kukeboard.article.service.request.ArticleCreateRequest;
import kuke.kukeboard.article.service.request.ArticleUpdateRequest;
import kuke.kukeboard.article.service.response.ArticleResponse;

/**
 * Web layer only: ArticleService is mocked, so no DB is involved. Verifies
 * request/response mapping between HTTP and ArticleController.
 */
@WebMvcTest(ArticleController.class)
@DisplayName("ArticleController 웹 계층 테스트 (MockMvc, 서비스는 Mock)")
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ArticleService articleService;

    private ArticleResponse sampleResponse(long articleId) {
        return new ArticleResponse(articleId, "title", "content", 1L, 1L, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /v1/articles -- 요청 바디를 그대로 서비스에 전달하고, 서비스 응답을 JSON으로 반환한다")
    void create() throws Exception {
        ArticleCreateRequest request = new ArticleCreateRequest("title", "content", 1L, 1L);
        given(articleService.create(request)).willReturn(sampleResponse(100L));

        mockMvc.perform(post("/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleId").value(100))
                .andExpect(jsonPath("$.title").value("title"));
    }

    @Test
    @DisplayName("GET /v1/articles -- 쿼리 파라미터를 그대로 서비스에 전달하고, 서비스 응답을 JSON으로 반환한다")
    void readAll() throws Exception {
        PageResponse<ArticleResponse> page = new PageResponse<>(List.of(sampleResponse(100L)), 1, 30, 10, 10, true);
        given(articleService.readAll(1L, 1L, 30L, 10L)).willReturn(page);

        mockMvc.perform(get("/v1/articles")
                        .param("boardId", "1")
                        .param("page", "1")
                        .param("pageSize", "30")
                        .param("pageLimit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.data[0].articleId").value(100));
    }

    @Test
    @DisplayName("GET /v1/articles/infinite-scroll -- 쿼리 파라미터를 그대로 서비스에 전달하고, 서비스 응답을 JSON으로 반환한다")
    void readAllInfiniteScroll() throws Exception {
        CursorResponse<ArticleResponse> page = new CursorResponse<>(List.of(sampleResponse(100L)), 100L, false);
        given(articleService.readAllInfiniteScroll(1L, null, 30L)).willReturn(page);

        mockMvc.perform(get("/v1/articles/infinite-scroll")
                        .param("boardId", "1")
                        .param("pageSize", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").value(100))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("GET /v1/articles/{articleId} -- 경로 변수를 서비스에 전달하고, 서비스 응답을 JSON으로 반환한다")
    void read() throws Exception {
        given(articleService.read(100L)).willReturn(sampleResponse(100L));

        mockMvc.perform(get("/v1/articles/{articleId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleId").value(100));
    }

    @Test
    @DisplayName("PUT /v1/articles/{articleId} -- 경로 변수와 요청 바디를 그대로 서비스에 전달한다")
    void update() throws Exception {
        ArticleUpdateRequest request = new ArticleUpdateRequest("new title", "new content");
        given(articleService.update(100L, request)).willReturn(sampleResponse(100L));

        mockMvc.perform(put("/v1/articles/{articleId}", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleId").value(100));
    }

    @Test
    @DisplayName("DELETE /v1/articles/{articleId} -- 경로 변수를 그대로 서비스에 전달한다")
    void deleteArticle() throws Exception {
        mockMvc.perform(delete("/v1/articles/{articleId}", 100L))
                .andExpect(status().isOk());

        verify(articleService).delete(100L);
    }
}
