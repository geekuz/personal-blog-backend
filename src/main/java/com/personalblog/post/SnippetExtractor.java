package com.personalblog.post;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a short plain-text excerpt around the first search match in a post body. Markdown syntax is
 * stripped, but code stays as text because readers search for identifiers inside code blocks.
 */
final class SnippetExtractor {
    static final int MAX_LENGTH = 160;
    private static final int CONTEXT_BEFORE_MATCH = 60;
    private static final Pattern FENCE = Pattern.compile("(?m)^\\s*(```|~~~).*$");
    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\([^)]*\\)");
    private static final Pattern HEADING = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s+");
    private static final Pattern BLOCKQUOTE = Pattern.compile("(?m)^\\s{0,3}>\\s?");
    private static final Pattern EMPHASIS_AND_CODE_MARKS = Pattern.compile("[*`]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SnippetExtractor() {}

    /** Returns null when no term occurs in the body, so callers fall back to the post summary. */
    static String snippet(String markdown, List<String> terms) {
        if (markdown == null || markdown.isBlank() || terms.isEmpty()) return null;
        String text = plainText(markdown);
        int matchStart = -1;
        int matchEnd = -1;
        for (String term : terms) {
            Matcher matcher = Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(text);
            if (matcher.find() && (matchStart < 0 || matcher.start() < matchStart)) {
                matchStart = matcher.start();
                matchEnd = matcher.end();
            }
        }
        if (matchStart < 0) return null;

        int start = Math.max(0, matchStart - CONTEXT_BEFORE_MATCH);
        int end = Math.min(text.length(), start + MAX_LENGTH);
        start = Math.max(0, end - MAX_LENGTH);
        if (start > 0) {
            int space = text.indexOf(' ', start);
            if (space >= 0 && space < matchStart) start = space + 1;
        }
        if (end < text.length()) {
            int space = text.lastIndexOf(' ', end);
            if (space >= matchEnd) end = space;
        }
        // Without a nearby space the cut falls at a fixed offset, which can split an emoji's surrogate pair.
        if (start > 0 && Character.isLowSurrogate(text.charAt(start))) start++;
        if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return (start > 0 ? "…" : "") + text.substring(start, end).strip() + (end < text.length() ? "…" : "");
    }

    private static String plainText(String markdown) {
        String text = FENCE.matcher(markdown).replaceAll(" ");
        text = IMAGE.matcher(text).replaceAll("");
        text = LINK.matcher(text).replaceAll("$1");
        text = HEADING.matcher(text).replaceAll("");
        text = BLOCKQUOTE.matcher(text).replaceAll("");
        text = EMPHASIS_AND_CODE_MARKS.matcher(text).replaceAll("");
        return WHITESPACE.matcher(text).replaceAll(" ").strip();
    }
}
