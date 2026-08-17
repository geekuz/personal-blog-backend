package com.personalblog.tag;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findBySlug(String slug);
    interface PublishedTagCount {
        String getName(); String getSlug(); long getPostCount();
    }

    @Query("""
        select t.name as name, t.slug as slug, count(p) as postCount
        from Post p join p.tags t
        where p.status = com.personalblog.post.PostStatus.PUBLISHED
        group by t.id, t.name, t.slug order by lower(t.name), t.slug
        """)
    List<PublishedTagCount> findPublishedTagCounts();
}
