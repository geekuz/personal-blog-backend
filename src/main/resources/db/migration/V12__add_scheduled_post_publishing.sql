alter table posts add column scheduled_at timestamp with time zone;

alter table posts drop constraint ck_posts_published_at;
alter table posts add constraint ck_posts_publication_dates check (
    (status = 'DRAFT' and published_at is null and scheduled_at is null)
    or (status = 'SCHEDULED' and published_at is null and scheduled_at is not null)
    or (status = 'PUBLISHED' and published_at is not null and scheduled_at is null)
);

alter table posts drop constraint if exists posts_status_check;
-- H2 names the original inline V1 check deterministically; production PostgreSQL uses posts_status_check.
alter table posts drop constraint if exists "CONSTRAINT_65";
alter table posts add constraint ck_posts_status check (status in ('DRAFT', 'SCHEDULED', 'PUBLISHED'));

create index idx_posts_scheduled_at on posts (status, scheduled_at);
