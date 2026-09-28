package com.personalblog.api.dto;

/** Lightweight pointer to a neighbouring published post; never embeds content. */
public record PostLinkResponse(String slug, String title) {}
