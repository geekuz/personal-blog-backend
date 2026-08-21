package com.personalblog.api.dto;

import java.util.List;

public record AdminDashboardResponse(
    long publishedPosts, long draftPosts, long subscribers,
    long pendingDeliveries, long failedDeliveries,
    List<AdminPostResponse> posts
) {}
