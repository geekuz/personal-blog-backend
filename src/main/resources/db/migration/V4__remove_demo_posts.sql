delete from posts
where id in (
    '20000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000004',
    '20000000-0000-0000-0000-000000000005'
);

delete from tags
where not exists (select 1 from post_tags where post_tags.tag_id = tags.id);
