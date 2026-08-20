package com.personalblog.comment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {
    @EntityGraph(attributePaths = "author")
    List<PostComment> findByPostId(UUID postId, Pageable pageable);
}
