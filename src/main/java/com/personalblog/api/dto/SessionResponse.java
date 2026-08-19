package com.personalblog.api.dto;

public record SessionResponse(boolean authenticated, UserResponse user) {}
