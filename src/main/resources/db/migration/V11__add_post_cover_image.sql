alter table posts add column cover_image_url varchar(2048);
alter table posts add column cover_image_alt varchar(300);

alter table posts add constraint ck_posts_cover_image_pair check (
    (cover_image_url is null and cover_image_alt is null)
    or (cover_image_url is not null and cover_image_alt is not null)
);
