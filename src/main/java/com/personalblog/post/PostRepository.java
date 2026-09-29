package com.personalblog.post;

import com.personalblog.api.dto.PostLinkResponse;
import com.personalblog.api.dto.RelatedPostResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.Instant;
import jakarta.persistence.LockModeType;

public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
    // Newest-first listing without a search query; searches go through PostSearchSpecifications.
    @Query(value = """
        select p from Post p
        where p.status = com.personalblog.post.PostStatus.PUBLISHED
          and (:tag is null or exists (select 1 from p.tags tf where tf.slug = :tag))
        """,
        countQuery = """
        select count(p) from Post p
        where p.status = com.personalblog.post.PostStatus.PUBLISHED
          and (:tag is null or exists (select 1 from p.tags tf where tf.slug = :tag))
        """)
    Page<Post> findPublished(@Param("tag") String tag, Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    @EntityGraph(attributePaths = "tags")
    Optional<Post> findBySlug(String slug);

    // Newest first, matching the public list ordering; tags batch-load lazily inside the caller's transaction.
    List<Post> findByStatusOrderByPublishedAtDescIdDesc(PostStatus status, Pageable pageable);

    @Query("""
        select new com.personalblog.post.PublishedPostRef(p.slug, p.updatedAt) from Post p
        where p.status = com.personalblog.post.PostStatus.PUBLISHED
        order by p.publishedAt desc, p.id desc
        """)
    List<PublishedPostRef> findPublishedRefs(Pageable pageable);

    // Neighbours follow the public list ordering (publishedAt desc, id desc) so navigation never skips or repeats.
    @Query("""
        select new com.personalblog.api.dto.PostLinkResponse(p.slug, p.title) from Post p
        where p.status = com.personalblog.post.PostStatus.PUBLISHED
          and (p.publishedAt < :publishedAt or (p.publishedAt = :publishedAt and p.id < :id))
        order by p.publishedAt desc, p.id desc
        """)
    List<PostLinkResponse> findOlderPublished(@Param("publishedAt") Instant publishedAt, @Param("id") UUID id,
                                              Pageable pageable);

    @Query("""
        select new com.personalblog.api.dto.PostLinkResponse(p.slug, p.title) from Post p
        where p.status = com.personalblog.post.PostStatus.PUBLISHED
          and (p.publishedAt > :publishedAt or (p.publishedAt = :publishedAt and p.id > :id))
        order by p.publishedAt asc, p.id asc
        """)
    List<PostLinkResponse> findNewerPublished(@Param("publishedAt") Instant publishedAt, @Param("id") UUID id,
                                              Pageable pageable);

    // Most shared tags first, then newest; posts sharing no tag with the given post are never returned.
    @Query("""
        select new com.personalblog.api.dto.RelatedPostResponse(p.slug, p.title, p.summary) from Post p join p.tags t
        where p.status = com.personalblog.post.PostStatus.PUBLISHED and p.id <> :id
          and t.id in (select ct.id from Post c join c.tags ct where c.id = :id)
        group by p.id, p.slug, p.title, p.summary, p.publishedAt
        order by count(t) desc, p.publishedAt desc, p.id desc
        """)
    List<RelatedPostResponse> findRelatedPublished(@Param("id") UUID id, Pageable pageable);

    boolean existsBySlug(String slug);
    boolean existsByCoverImageUrl(String coverImageUrl);
    long countByStatus(PostStatus status);
    @EntityGraph(attributePaths = "tags")
    List<Post> findAllByOrderByUpdatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.status = com.personalblog.post.PostStatus.SCHEDULED and p.scheduledAt <= :now order by p.scheduledAt asc")
    List<Post> findDueScheduled(@Param("now") Instant now, Pageable pageable);
}
