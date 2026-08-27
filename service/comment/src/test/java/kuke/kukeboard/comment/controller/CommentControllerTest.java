package kuke.kukeboard.comment.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import kuke.kukeboard.comment.service.CommentService;
import kuke.kukeboard.comment.service.request.CommentCreateRequest;
import kuke.kukeboard.comment.service.response.CommentResponse;

/**
 * Web layer only: CommentService is mocked, so no DB (real or
 * Testcontainers) is involved. Verifies request/response mapping between
 * HTTP and CommentController -- much cheaper than a full E2E test.
 */
@WebMvcTest(CommentController.class)
@DisplayName("CommentController 웹 계층 테스트 (MockMvc, 서비스는 Mock)")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CommentService commentService;

    @Test
    @DisplayName("POST /v1/comments -- 요청 바디를 그대로 서비스에 전달하고, 서비스 응답을 JSON으로 반환한다")
    void create() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest("hello", 1L, 2L, null);
        CommentResponse response = new CommentResponse(100L, "hello", 1L, 100L, 2L, false, LocalDateTime.now());
        given(commentService.create(request)).willReturn(response);

        mockMvc.perform(post("/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(100))
                .andExpect(jsonPath("$.content").value("hello"))
                .andExpect(jsonPath("$.parentCommentId").value(100));
    }

    @Test
    @DisplayName("GET /v1/comments/{commentId} -- 경로 변수를 서비스에 전달하고, 서비스 응답을 JSON으로 반환한다")
    void read() throws Exception {
        CommentResponse response = new CommentResponse(100L, "hello", 1L, 100L, 2L, false, LocalDateTime.now());
        given(commentService.read(100L)).willReturn(response);

        mockMvc.perform(get("/v1/comments/{commentId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(100))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    @DisplayName("DELETE /v1/comments/{commentId} -- 경로 변수를 그대로 서비스에 전달한다")
    void deleteComment() throws Exception {
        mockMvc.perform(delete("/v1/comments/{commentId}", 100L))
                .andExpect(status().isOk());

        verify(commentService).delete(100L);
    }
}
