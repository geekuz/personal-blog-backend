create table email_verification_tokens (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(64) not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    created_at timestamp with time zone not null,
    constraint uq_email_verification_token_hash unique (token_hash)
);

create index idx_email_verification_tokens_user on email_verification_tokens(user_id);
create index idx_email_verification_tokens_expiry on email_verification_tokens(expires_at);
