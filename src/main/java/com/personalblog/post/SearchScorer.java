package com.personalblog.post;

import com.personalblog.tag.Tag;
import java.util.List;
import java.util.Locale;

/** Relevance score for a post that already matches every term: title hits outweigh tags, summary, then body. */
final class SearchScorer {
    private static final int TITLE_WEIGHT = 10;
    private static final int TAG_WEIGHT = 6;
    private static final int SUMMARY_WEIGHT = 4;
    private static final int MAX_BODY_OCCURRENCES_COUNTED = 4;

    private SearchScorer() {}

    static int score(Post post, List<String> terms) {
        String title = lower(post.getTitle());
        String summary = lower(post.getSummary());
        String body = lower(post.getContent());
        int score = 0;
        for (String term : terms) {
            if (title.contains(term)) score += TITLE_WEIGHT;
            if (matchesTag(post, term)) score += TAG_WEIGHT;
            if (summary.contains(term)) score += SUMMARY_WEIGHT;
            score += Math.min(occurrences(body, term), MAX_BODY_OCCURRENCES_COUNTED);
        }
        return score;
    }

    private static boolean matchesTag(Post post, String term) {
        for (Tag tag : post.getTags()) {
            if (lower(tag.getName()).contains(term) || tag.getSlug().contains(term)) return true;
        }
        return false;
    }

    private static int occurrences(String text, String term) {
        int count = 0;
        for (int from = text.indexOf(term); from >= 0 && count < MAX_BODY_OCCURRENCES_COUNTED;
             from = text.indexOf(term, from + term.length())) {
            count++;
        }
        return count;
    }

    private static String lower(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
}
