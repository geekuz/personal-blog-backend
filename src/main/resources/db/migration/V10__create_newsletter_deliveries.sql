create table newsletter_deliveries (
    id uuid primary key,
    post_id uuid not null references posts(id) on delete cascade,
    subscription_id uuid not null references newsletter_subscriptions(id) on delete cascade,
    status varchar(16) not null check (status in ('PENDING', 'SENDING', 'SENT', 'FAILED')),
    attempts integer not null default 0 check (attempts >= 0),
    next_attempt_at timestamp with time zone not null,
    sent_at timestamp with time zone,
    last_error varchar(500),
    created_at timestamp with time zone not null,
    constraint uq_newsletter_deliveries_post_subscription unique (post_id, subscription_id)
);

create index idx_newsletter_deliveries_dispatch
    on newsletter_deliveries(status, next_attempt_at, created_at);

