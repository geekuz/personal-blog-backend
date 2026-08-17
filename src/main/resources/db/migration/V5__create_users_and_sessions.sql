create table users (
    id uuid primary key,
    email varchar(254) not null,
    password_hash varchar(100) not null,
    display_name varchar(80) not null,
    enabled boolean not null default true,
    email_verified boolean not null default false,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_users_email unique (email)
);

create table user_roles (
    user_id uuid not null references users(id) on delete cascade,
    role varchar(20) not null check (role in ('USER', 'ADMIN')),
    primary key (user_id, role)
);

create index idx_users_email on users(email);

create table spring_session (
    primary_id char(36) not null,
    session_id char(36) not null,
    creation_time bigint not null,
    last_access_time bigint not null,
    max_inactive_interval integer not null,
    expiry_time bigint not null,
    principal_name varchar(254),
    constraint pk_spring_session primary key (primary_id),
    constraint uq_spring_session_id unique (session_id)
);

create index idx_spring_session_expiry on spring_session(expiry_time);
create index idx_spring_session_principal on spring_session(principal_name);

create table spring_session_attributes (
    session_primary_id char(36) not null references spring_session(primary_id) on delete cascade,
    attribute_name varchar(200) not null,
    attribute_bytes bytea not null,
    primary key (session_primary_id, attribute_name)
);
