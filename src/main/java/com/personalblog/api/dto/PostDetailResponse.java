package com.personalblog.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * previousPost is the next-older published post and nextPost the next-newer one, following the public list
 * ordering (publishedAt desc, id desc); either is null at the ends of the list.
 */
public record PostDetailResponse(String slug, String title, String summary, String content,
                                 String coverImageUrl, String coverImageAlt, List<String> tags,
                                 Instant publishedAt, Instant updatedAt, int readingTimeMinutes,
                                 PostLinkResponse previousPost, PostLinkResponse nextPost,
                                 List<RelatedPostResponse> relatedPosts) {}
