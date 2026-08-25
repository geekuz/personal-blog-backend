package com.personalblog.post;

import com.personalblog.newsletter.NewsletterDeliveryService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduledPostPublisher {
    private static final Logger log = LoggerFactory.getLogger(ScheduledPostPublisher.class);
    private final PostRepository posts;
    private final NewsletterDeliveryService newsletter;

    public ScheduledPostPublisher(PostRepository posts, NewsletterDeliveryService newsletter) {
        this.posts = posts;
        this.newsletter = newsletter;
    }

    @Scheduled(fixedDelayString = "${blog.posts.publish-delay-ms:15000}")
    @Transactional
    public void publishDuePosts() {
        Instant now = Instant.now();
        List<Post> due = posts.findDueScheduled(now, PageRequest.of(0, 25));
        for (Post post : due) {
            post.publish(now);
            newsletter.enqueue(post);
            log.info("Published scheduled post {}", post.getId());
        }
    }
}
