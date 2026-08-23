package kuke.kukeboard.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kuke.kukeboard.article.entity.Article;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
