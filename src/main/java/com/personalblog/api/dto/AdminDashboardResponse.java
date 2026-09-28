package com.personalblog.api.dto;

import java.util.List;
import com.personalblog.media.MediaUploadResponse;

public record AdminDashboardResponse(
    long publishedPosts, long draftPosts, long scheduledPosts, long subscribers,
    long pendingDeliveries, long failedDeliveries,
    List<AdminPostResponse> posts,
    List<MediaUploadResponse> media
) {}
