package com.personalblog.api.dto;

/** A published post sharing tags with the one being read; never embeds content. */
public record RelatedPostResponse(String slug, String title, String summary) {}
