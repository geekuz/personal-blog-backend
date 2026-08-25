package com.personalblog.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personalblog.newsletter.NewsletterDeliveryService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ScheduledPostPublisherTest {
    @Test
    void publishesDuePostsAndQueuesNewsletter() {
        PostRepository posts = org.mockito.Mockito.mock(PostRepository.class);
        NewsletterDeliveryService newsletter = org.mockito.Mockito.mock(NewsletterDeliveryService.class);
        Instant scheduledAt = Instant.now().minusSeconds(60);
        Post post = new Post("scheduled", "Scheduled", "Summary", "Body", null, null,
            PostStatus.SCHEDULED, null, scheduledAt, scheduledAt, Set.of());
        when(posts.findDueScheduled(any(Instant.class), any(Pageable.class))).thenReturn(List.of(post));

        new ScheduledPostPublisher(posts, newsletter).publishDuePosts();

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isNotNull();
        assertThat(post.getScheduledAt()).isNull();
        verify(newsletter).enqueue(post);
    }
}
