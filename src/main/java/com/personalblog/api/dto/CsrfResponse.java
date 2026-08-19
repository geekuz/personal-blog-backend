package com.personalblog.api.dto;

public record CsrfResponse(String headerName, String token) {}
