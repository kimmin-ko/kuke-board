package kuke.kukeboard.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kuke.kukeboard.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    boolean existsByParentCommentIdAndCommentIdNot(Long parentCommentId, Long commentId);
}
