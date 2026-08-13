package com.personalblog.post;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class PostServiceTest {
    @Test void readingTimeHasOneMinuteMinimum() {
        assertThat(PostService.readingTime("")).isEqualTo(1);
        assertThat(PostService.readingTime("one two three")).isEqualTo(1);
    }

    @Test void readingTimeRoundsAtTwoHundredWordsPerMinute() {
        assertThat(PostService.readingTime("word ".repeat(299))).isEqualTo(1);
        assertThat(PostService.readingTime("word ".repeat(300))).isEqualTo(2);
        assertThat(PostService.readingTime("word ".repeat(500))).isEqualTo(3);
    }
}
