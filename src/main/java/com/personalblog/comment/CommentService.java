package com.personalblog.comment;

import com.personalblog.api.dto.CommentListResponse;
import com.personalblog.api.dto.CommentResponse;
import com.personalblog.post.Post;
import com.personalblog.post.PostNotFoundException;
import com.personalblog.post.PostRepository;
import com.personalblog.post.PostStatus;
import com.personalblog.user.BlogUser;
import com.personalblog.user.BlogUserService;
import com.personalblog.user.UserRole;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {
    private static final int MAX_VISIBLE_COMMENTS = 50;
    private final PostCommentRepository comments;
    private final PostRepository posts;
    private final BlogUserService users;

    public CommentService(PostCommentRepository comments, PostRepository posts, BlogUserService users) {
        this.comments = comments;
        this.posts = posts;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public CommentListResponse list(String slug, String viewerEmail) {
        Post post = publishedPost(slug);
        BlogUser viewer = viewerEmail == null ? null : users.byEmail(viewerEmail);
        List<CommentResponse> items = comments.findByPostId(post.getId(), PageRequest.of(0,
                MAX_VISIBLE_COMMENTS, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).stream()
            .sorted(Comparator.comparing(PostComment::getCreatedAt))
            .map(comment -> response(comment, canDelete(comment, viewer)))
            .toList();
        return new CommentListResponse(items);
    }

    public CommentResponse create(String slug, String email, String body) {
        Post post = publishedPost(slug);
        BlogUser author = users.byEmail(email);
        if (!author.isEmailVerified()) throw new CommentEmailVerificationRequiredException();
        PostComment comment = comments.save(new PostComment(post, author, body.trim(), Instant.now()));
        return response(comment, true);
    }

    public void delete(UUID commentId, String email) {
        PostComment comment = comments.findById(commentId).orElseThrow(CommentNotFoundException::new);
        BlogUser viewer = users.byEmail(email);
        if (!canDelete(comment, viewer)) throw new CommentForbiddenException();
        comments.delete(comment);
    }

    private Post publishedPost(String slug) {
        return posts.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
            .orElseThrow(() -> new PostNotFoundException(slug));
    }

    private boolean canDelete(PostComment comment, BlogUser viewer) {
        return viewer != null && (comment.getAuthor().getId().equals(viewer.getId())
            || viewer.getRoles().contains(UserRole.ADMIN));
    }

    private CommentResponse response(PostComment comment, boolean canDelete) {
        return new CommentResponse(comment.getId(), comment.getAuthor().getDisplayName(),
            comment.getBody(), comment.getCreatedAt(), canDelete);
    }
}
