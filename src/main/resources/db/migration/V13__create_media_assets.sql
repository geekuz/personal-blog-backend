create table media_assets (
    id uuid primary key,
    object_key varchar(512) not null unique,
    public_url varchar(2048) not null unique,
    original_filename varchar(255) not null,
    content_type varchar(64) not null,
    size_bytes bigint not null check (size_bytes > 0),
    width integer not null check (width > 0),
    height integer not null check (height > 0),
    created_at timestamp with time zone not null
);

create index idx_media_assets_created_at on media_assets (created_at desc);
