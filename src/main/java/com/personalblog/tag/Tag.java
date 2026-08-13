package com.personalblog.tag;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "tags")
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 50) private String name;
    @Column(nullable = false, unique = true, length = 50) private String slug;

    protected Tag() {}
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
}
