alter table newsletter_subscriptions add column email varchar(254);
alter table newsletter_subscriptions add column display_name varchar(80);
alter table newsletter_subscriptions add column confirmed_at timestamp with time zone;

update newsletter_subscriptions subscription
set email = blog_user.email,
    display_name = blog_user.display_name,
    confirmed_at = subscription.subscribed_at
from users blog_user
where blog_user.id = subscription.user_id;

alter table newsletter_subscriptions alter column email set not null;
alter table newsletter_subscriptions alter column display_name set not null;
alter table newsletter_subscriptions alter column user_id drop not null;

create unique index uq_newsletter_subscriptions_email
    on newsletter_subscriptions (email);

create table newsletter_subscription_tokens (
    id uuid primary key,
    subscription_id uuid not null references newsletter_subscriptions(id) on delete cascade,
    purpose varchar(16) not null check (purpose in ('CONFIRM', 'UNSUBSCRIBE')),
    token_hash varchar(64) not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    created_at timestamp with time zone not null,
    constraint uq_newsletter_subscription_tokens_hash unique (token_hash)
);

create index idx_newsletter_subscription_tokens_subscription
    on newsletter_subscription_tokens(subscription_id, purpose, created_at desc);
