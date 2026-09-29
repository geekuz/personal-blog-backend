package com.personalblog.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PostSearchTest {
    @Test void parsesWordsAndQuotedPhrasesLowercasedAndDeduplicated() {
        assertThat(SearchTerms.parse("  React \"Custom Hooks\" react useEffect ").terms())
            .containsExactly("react", "custom hooks", "useeffect");
    }

    @Test void ignoresBlankInputAndEmptyQuotes() {
        assertThat(SearchTerms.parse(null).isEmpty()).isTrue();
        assertThat(SearchTerms.parse("   \"\"  ").isEmpty()).isTrue();
    }

    @Test void capsTheNumberOfTerms() {
        assertThat(SearchTerms.parse("a b c d e f g h i j").terms()).hasSize(SearchTerms.MAX_TERMS);
    }

    @Test void escapesLikeWildcards() {
        assertThat(SearchTerms.likePattern("100%_a\\b")).isEqualTo("%100\\%\\_a\\\\b%");
    }

    @Test void snippetShowsContextAroundTheFirstMatchWithEllipses() {
        String body = "word ".repeat(60) + "the needle sits here " + "tail ".repeat(60);
        String snippet = SnippetExtractor.snippet(body, List.of("needle"));
        assertThat(snippet).startsWith("…").endsWith("…").contains("the needle sits here");
        assertThat(snippet.length()).isLessThanOrEqualTo(SnippetExtractor.MAX_LENGTH + 2);
    }

    @Test void snippetStripsMarkdownButKeepsCodeText() {
        String body = """
            # Heading
            See [the docs](https://example.com) and ![diagram](x.png).

            ```java
            @Transactional void save() {}
            ```
            """;
        String snippet = SnippetExtractor.snippet(body, List.of("@transactional"));
        assertThat(snippet).contains("See the docs and").contains("@Transactional void save()")
            .doesNotContain("```").doesNotContain("](").doesNotContain("#");
    }

    @Test void snippetIsNullWhenNoTermAppearsInTheBody() {
        assertThat(SnippetExtractor.snippet("nothing relevant", List.of("hooks"))).isNull();
        assertThat(SnippetExtractor.snippet(null, List.of("hooks"))).isNull();
    }

    @Test void snippetNeverSplitsAnEmojiAtItsEdges() {
        // No spaces, so the cut falls at fixed offsets; the odd-length prefix lands it inside a surrogate pair.
        String body = "😀".repeat(100) + "xneedle" + "😀".repeat(100);
        String snippet = SnippetExtractor.snippet(body, List.of("needle"));
        assertThat(snippet).contains("needle");
        assertThat(snippet.codePoints().filter(cp -> cp >= 0xD800 && cp <= 0xDFFF)).isEmpty();
    }
}
