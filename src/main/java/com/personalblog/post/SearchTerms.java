package com.personalblog.post;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A search query split into lowercase terms. Words are separated by whitespace and "quoted phrases" stay
 * together. Terms are matched as substrings, not dictionary words, so code identifiers such as
 * {@code useEffect} or {@code @Transactional} are found exactly as written. A phrase's inner whitespace becomes
 * one space, so it will not match text where those words are split across a line break.
 */
public record SearchTerms(List<String> terms) {
    public static final int MAX_TERMS = 8;
    static final int MAX_TERM_LENGTH = 100;
    private static final Pattern TOKEN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    public SearchTerms {
        terms = List.copyOf(terms);
    }

    public static SearchTerms parse(String raw) {
        if (raw == null || raw.isBlank()) return new SearchTerms(List.of());
        Set<String> unique = new LinkedHashSet<>();
        Matcher matcher = TOKEN.matcher(raw);
        while (matcher.find() && unique.size() < MAX_TERMS) {
            String token = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String term = token.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (term.length() > MAX_TERM_LENGTH) term = term.substring(0, MAX_TERM_LENGTH);
            if (!term.isEmpty()) unique.add(term);
        }
        return new SearchTerms(List.copyOf(unique));
    }

    public boolean isEmpty() { return terms.isEmpty(); }

    /** A LIKE pattern matching the term anywhere, with {@code \} as the escape character. */
    static String likePattern(String term) {
        String escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
