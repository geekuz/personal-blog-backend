insert into tags (id, name, slug) values
 ('10000000-0000-0000-0000-000000000001', 'React', 'react'),
 ('10000000-0000-0000-0000-000000000002', 'Learning', 'learning'),
 ('10000000-0000-0000-0000-000000000003', 'Java', 'java'),
 ('10000000-0000-0000-0000-000000000004', 'Spring Boot', 'spring-boot');

insert into posts (id, slug, title, summary, content, status, published_at, created_at, updated_at) values
 ('20000000-0000-0000-0000-000000000001', 'why-i-chose-react', 'Why I Chose React to Learn First', 'A quick take on why React is a solid first framework.', '# Why React?

React has a small core model and an enormous ecosystem. Learning components, props, and state gave me a practical path from static pages to interactive applications.', 'PUBLISHED', '2026-06-18T09:00:00Z', '2026-06-18T09:00:00Z', '2026-06-18T09:00:00Z'),
 ('20000000-0000-0000-0000-000000000002', 'learning-java-21', 'Learning Java 21', 'Notes from learning modern Java one feature at a time.', '# Modern Java

Records, pattern matching, and virtual threads make Java expressive while retaining its mature tooling and runtime.', 'PUBLISHED', '2026-07-02T10:00:00Z', '2026-07-02T10:00:00Z', '2026-07-02T10:00:00Z'),
 ('20000000-0000-0000-0000-000000000003', 'building-a-blog-api', 'Building a Blog API with Spring Boot', 'Design choices behind this small, read-only blog API.', '# A focused API

The first release exposes published posts and tags. PostgreSQL stores Markdown source, Flyway owns the schema, and DTOs keep persistence details private.', 'PUBLISHED', '2026-08-01T08:30:00Z', '2026-08-01T08:30:00Z', '2026-08-01T08:30:00Z');

insert into post_tags (post_id, tag_id) values
 ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001'),
 ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002'),
 ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002'),
 ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003'),
 ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003'),
 ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004');
