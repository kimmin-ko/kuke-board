package kuke.kukeboard.article.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kuke.kukeboard.article.entity.Article;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    /**
     * Step 1 of the deferred join: covering-index-only id lookup.
     */
    @Query("select a.articleId from Article a where a.boardId = :boardId order by a.articleId desc")
    List<Long> findIds(@Param("boardId") Long boardId, Pageable pageable);

    /**
     * Step 2: full rows for exactly the ids resolved above.
     */
    @Query("select a from Article a where a.articleId in :articleIds order by a.articleId desc")
    List<Article> findAllByIds(@Param("articleIds") Collection<Long> articleIds);

    /**
     * Cursor(keyset) pagination: always seeks directly via
     * idx_board_id_article_id, so cost stays O(pageSize) regardless of depth.
     */
    @Query("""
            select a
            from Article a
            where a.boardId = :boardId
            and (:lastArticleId is null or a.articleId < :lastArticleId)
            order by a.articleId desc
            """)
    List<Article> findAllByCursor(
            @Param("boardId") Long boardId,
            @Param("lastArticleId") Long lastArticleId,
            Pageable pageable
    );
}
