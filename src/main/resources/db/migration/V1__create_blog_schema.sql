create table posts (
    id uuid primary key,
    slug varchar(160) not null,
    title varchar(200) not null,
    summary varchar(500) not null,
    content text not null,
    status varchar(16) not null check (status in ('DRAFT', 'PUBLISHED')),
    published_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_posts_slug unique (slug),
    constraint ck_posts_published_at check (status <> 'PUBLISHED' or published_at is not null)
);

create table tags (
    id uuid primary key,
    name varchar(50) not null,
    slug varchar(50) not null,
    constraint uq_tags_slug unique (slug)
);

create table post_tags (
    post_id uuid not null references posts(id) on delete cascade,
    tag_id uuid not null references tags(id) on delete cascade,
    primary key (post_id, tag_id)
);

create index idx_posts_public_order on posts(status, published_at desc, id desc);
create index idx_post_tags_tag on post_tags(tag_id);
create index idx_post_tags_post on post_tags(post_id);
