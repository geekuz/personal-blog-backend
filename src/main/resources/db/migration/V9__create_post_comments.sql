create table post_comments (
    id uuid primary key,
    post_id uuid not null references posts(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    body varchar(2000) not null,
    created_at timestamp with time zone not null,
    constraint ck_post_comments_body_not_blank check (length(trim(body)) > 0)
);

create index idx_post_comments_post_created
    on post_comments(post_id, created_at desc, id desc);
create index idx_post_comments_user on post_comments(user_id);
