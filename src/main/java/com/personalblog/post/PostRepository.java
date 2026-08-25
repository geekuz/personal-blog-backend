package com.personalblog.post;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.Instant;
import jakarta.persistence.LockModeType;

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

    @EntityGraph(attributePaths = "tags")
    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);
    long countByStatus(PostStatus status);
    @EntityGraph(attributePaths = "tags")
    List<Post> findAllByOrderByUpdatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.status = com.personalblog.post.PostStatus.SCHEDULED and p.scheduledAt <= :now order by p.scheduledAt asc")
    List<Post> findDueScheduled(@Param("now") Instant now, Pageable pageable);
}
