package com.personalblog.post;

import com.personalblog.tag.Tag;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.jpa.domain.Specification;

/** Published posts where every term appears in the title, summary, body, or a tag name. */
final class PostSearchSpecifications {
    private static final char LIKE_ESCAPE = '\\';

    private PostSearchSpecifications() {}

    static Specification<Post> publishedMatchingAll(SearchTerms terms, String tagSlug) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), PostStatus.PUBLISHED));
            if (tagSlug != null) {
                predicates.add(cb.exists(tagSubquery(root, query, cb, tag -> cb.equal(tag.get("slug"), tagSlug))));
            }
            for (String term : terms.terms()) {
                String pattern = SearchTerms.likePattern(term);
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("summary")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("content")), pattern, LIKE_ESCAPE),
                    cb.exists(tagSubquery(root, query, cb, tag -> cb.or(
                        cb.like(cb.lower(tag.get("name")), pattern, LIKE_ESCAPE),
                        cb.like(tag.get("slug"), pattern, LIKE_ESCAPE))))));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Subquery<Integer> tagSubquery(Root<Post> post, CriteriaQuery<?> query, CriteriaBuilder cb,
                                                 Function<Join<Post, Tag>, Predicate> condition) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<Post> tagged = subquery.from(Post.class);
        Join<Post, Tag> tag = tagged.join("tags");
        return subquery.select(cb.literal(1)).where(cb.equal(tagged, post), condition.apply(tag));
    }
}
