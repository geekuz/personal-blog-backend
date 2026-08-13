package com.personalblog.api.dto;

import java.util.List;

public record PostPageResponse(List<PostSummaryResponse> items, int page, int size, long totalItems,
                               int totalPages, boolean hasNext) {}
