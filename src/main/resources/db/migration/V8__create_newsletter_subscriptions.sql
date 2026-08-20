create table newsletter_subscriptions (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    subscribed_at timestamp with time zone not null,
    constraint uq_newsletter_subscriptions_user unique (user_id)
);

create index idx_newsletter_subscriptions_subscribed_at
    on newsletter_subscriptions(subscribed_at);
