package kuke.kukeboard.comment.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kuke.board.common.pagination.PageResponse;
import kuke.kukeboard.comment.service.CommentService;
import kuke.kukeboard.comment.service.request.CommentCreateRequest;
import kuke.kukeboard.comment.service.response.CommentCursorResponse;
import kuke.kukeboard.comment.service.response.CommentResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public CommentResponse create(@RequestBody CommentCreateRequest request) {
        return commentService.create(request);
    }

    /**
     * Page-number-block pagination, root-first then replies oldest-first
     * within each root.
     */
    @GetMapping
    public PageResponse<CommentResponse> readAll(
            @RequestParam Long articleId,
            @RequestParam Long page,
            @RequestParam Long pageSize,
            @RequestParam(defaultValue = "10") Long pageLimit
    ) {
        return commentService.readAll(articleId, page, pageSize, pageLimit);
    }

    /**
     * Cursor(keyset) pagination for infinite scroll. Pass both
     * lastParentCommentId and lastCommentId back from the previous
     * response's cursor fields -- omit both for the first page.
     */
    @GetMapping("/infinite-scroll")
    public CommentCursorResponse<CommentResponse> readAllInfiniteScroll(
            @RequestParam Long articleId,
            @RequestParam(required = false) Long lastParentCommentId,
            @RequestParam(required = false) Long lastCommentId,
            @RequestParam Long pageSize
    ) {
        return commentService.readAllInfiniteScroll(articleId, lastParentCommentId, lastCommentId, pageSize);
    }

    @GetMapping("/{commentId}")
    public CommentResponse read(@PathVariable Long commentId) {
        return commentService.read(commentId);
    }

    @DeleteMapping("/{commentId}")
    public void delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
    }
}
