package kuke.kukeboard.comment.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kuke.kukeboard.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    boolean existsByParentCommentIdAndCommentIdNot(Long parentCommentId, Long commentId);

    /**
     * Step 1 of the deferred join: covering-index-only id lookup, ordered
     * root-first then replies oldest-first within each root (root's own
     * commentId always sorts before its replies' commentIds).
     */
    @Query("""
            select c.commentId
            from Comment c
            where c.articleId = :articleId
            order by c.parentCommentId asc, c.commentId asc
            """)
    List<Long> findIds(@Param("articleId") Long articleId, Pageable pageable);

    /**
     * Step 2: full rows for exactly the ids resolved above, re-sorted with
     * the same key so the result preserves step 1's order.
     */
    @Query("""
            select c
            from Comment c
            where c.commentId in :commentIds
            order by c.parentCommentId asc, c.commentId asc
            """)
    List<Comment> findAllByIds(@Param("commentIds") Collection<Long> commentIds);

    /**
     * Cursor(keyset) pagination: seeks directly via
     * idx_article_id_parent_comment_id_comment_id using a composite cursor
     * (lastParentCommentId, lastCommentId), so cost stays O(pageSize)
     * regardless of depth. A null lastParentCommentId means "from the start".
     */
    @Query("""
            select c
            from Comment c
            where c.articleId = :articleId
            and (
                :lastParentCommentId is null
                or c.parentCommentId > :lastParentCommentId
                or (c.parentCommentId = :lastParentCommentId and c.commentId > :lastCommentId)
            )
            order by c.parentCommentId asc, c.commentId asc
            """)
    List<Comment> findAllByCursor(
            @Param("articleId") Long articleId,
            @Param("lastParentCommentId") Long lastParentCommentId,
            @Param("lastCommentId") Long lastCommentId,
            Pageable pageable
    );
}
