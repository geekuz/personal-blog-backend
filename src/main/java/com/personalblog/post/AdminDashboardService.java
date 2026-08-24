package com.personalblog.post;

import com.personalblog.api.dto.AdminDashboardResponse;
import com.personalblog.api.dto.AdminPostResponse;
import com.personalblog.api.dto.TagInput;
import com.personalblog.newsletter.NewsletterDeliveryRepository;
import com.personalblog.newsletter.NewsletterDeliveryStatus;
import com.personalblog.newsletter.NewsletterSubscriptionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {
    private final PostRepository posts;
    private final NewsletterSubscriptionRepository subscriptions;
    private final NewsletterDeliveryRepository deliveries;

    public AdminDashboardService(PostRepository posts, NewsletterSubscriptionRepository subscriptions,
            NewsletterDeliveryRepository deliveries) {
        this.posts = posts; this.subscriptions = subscriptions; this.deliveries = deliveries;
    }

    public AdminDashboardResponse dashboard() {
        List<AdminPostResponse> items = posts.findAllByOrderByUpdatedAtDesc().stream().map(this::response).toList();
        return new AdminDashboardResponse(posts.countByStatus(PostStatus.PUBLISHED),
            posts.countByStatus(PostStatus.DRAFT), subscriptions.count(),
            deliveries.countByStatus(NewsletterDeliveryStatus.PENDING),
            deliveries.countByStatus(NewsletterDeliveryStatus.FAILED), items);
    }

    private AdminPostResponse response(Post post) {
        List<TagInput> tags = post.getTags().stream().map(tag -> new TagInput(tag.getName(), tag.getSlug())).toList();
        return new AdminPostResponse(post.getId(), post.getSlug(), post.getTitle(), post.getSummary(),
            post.getContent(), post.getCoverImageUrl(), post.getCoverImageAlt(), post.getStatus(),
            post.getPublishedAt(), post.getCreatedAt(), post.getUpdatedAt(), tags);
    }
}
