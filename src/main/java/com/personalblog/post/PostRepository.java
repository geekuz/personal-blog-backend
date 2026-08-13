package com.personalblog.post;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @Query(value = """
        select distinct p from Post p left join p.tags t
        where p.status = com.personalblog.post.PostStatus.PUBLISHED
          and (:q is null or lower(p.title) like lower(concat('%', :q, '%')) escape '\\'
               or lower(p.summary) like lower(concat('%', :q, '%')) escape '\\')
          and (:tag is null or exists (select 1 from p.tags tf where tf.slug = :tag))
        """,
        countQuery = """
        select count(p) from Post p
        where p.status = com.personalblog.post.PostStatus.PUBLISHED
          and (:q is null or lower(p.title) like lower(concat('%', :q, '%')) escape '\\'
               or lower(p.summary) like lower(concat('%', :q, '%')) escape '\\')
          and (:tag is null or exists (select 1 from p.tags tf where tf.slug = :tag))
        """)
    Page<Post> findPublished(@Param("q") String q, @Param("tag") String tag, Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);
}
