-- Users
CREATE TABLE users (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    name text default null,
    screen_name text NOT NULL unique,
    email text NOT NULL unique,
    validated boolean default false,
    type text default 'normal',
    oauth_type text default null,
    oauth_id text default null,
    twitter_handle text default null,
    github_handle text default null,
    language text default null,
    bio text default null,
    website text default null,
    karma integer not null default 1,
    block boolean DEFAULT false,
    stared_groups uuid[] default null,
    stared_channels uuid[] default null,
    last_seen_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'),
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));

  ALTER TABLE users ADD CONSTRAINT users_oauth_id_key UNIQUE (oauth_id);
  ALTER TABLE users ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
  ALTER TABLE users ADD CONSTRAINT last_seen_at_chk CHECK (EXTRACT(TIMEZONE from last_seen_at) = '0');
  CREATE INDEX users_created_at_index ON users(created_at DESC);

-- Groups
CREATE TABLE groups (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    name text unique not null,
    user_id UUID NOT NULL,
    privacy text not null default 'public',
    week_posts_count integer DEFAULT 0 NOT NULL,
    del boolean default false,
    purpose text default null,
    rule text default null,
    stars integer not null default 0,
    type text not null default 'unknown',
    channels uuid[] default null,
    admins text[] not null,
    related_groups text[] not null,
    cover_settings text default null,
    logo text default null,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE groups ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');

CREATE TABLE channels (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    name text not null default 'general',
    user_id UUID NOT NULL,
    group_id UUID NOT NULL,
    is_private boolean default false,
    del boolean default false,
    purpose text default null,
    stars integer not null default 0,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE channels ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');

CREATE TABLE posts (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    user_id UUID NOT NULL,
    user_screen_name text NOT NULL,
    group_id UUID default null,
    group_name text default null,
    channel_id UUID default null,
    channel_name text default null,
    is_private boolean not null default false,
    is_wiki boolean not null default false,
    title text not null,
    body text not null,
    body_format text not null default 'asciidoc',
    cover text default null,
    link text default null,
    lang text default null,
    data text default null,
    tops integer not null default 0,
    views bigint not null default 0,
    rank numeric(17,10) not null DEFAULT 0.00,
    comments_count integer not null default 0,
    is_serial boolean default false,
    del boolean default false,
    permalink text unique,
    choices text default null,
    poll_closed boolean default false,
    is_draft boolean default true,
    video text,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'),
            updated_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'),
            tags text[] default null,
    last_reply_at timestamp with time zone default null,
    last_reply_by text default null
    );
  ALTER TABLE posts ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
  ALTER TABLE posts ADD CONSTRAINT updated_at_chk CHECK (EXTRACT(TIMEZONE from updated_at) = '0');
  ALTER TABLE posts ADD CONSTRAINT last_reply_at_chk CHECK (EXTRACT(TIMEZONE from last_reply_at) = '0');

-- TODO: ensure composite uniqueness (entity_id, entity_type, idx)
CREATE TABLE comments (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    user_id UUID NOT NULL,
    post_id UUID default NULL,
    post_permalink text default NULL,
    body text not null,
    likes integer not null default 0,
    reply_to UUID default null,
    replies_count integer not null default 0,
    del boolean default false,
    idx integer not null default 1,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'),
    updated_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC')
    );
  ALTER TABLE comments ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
  ALTER TABLE comments ADD CONSTRAINT updated_at_chk CHECK (EXTRACT(TIMEZONE from updated_at) = '0');

CREATE TABLE refresh_tokens (
    user_id UUID NOT NULL unique,
    token   UUID NOT NULL unique);

CREATE TABLE stars (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    user_id UUID not null,
    object_type text not null,
    object_id UUID NOT NULL,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE stars ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
  ALTER TABLE stars ADD CONSTRAINT user_id_object UNIQUE (user_id,object_type,object_id);

CREATE TABLE bookmarks (
  id UUID DEFAULT uuid_generate_v4() primary key,
  flake_id bigint not null,
  user_id UUID not null,
  post_id UUID default null,
  item_id UUID default null,
  created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
ALTER TABLE bookmarks ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
ALTER TABLE bookmarks ADD CONSTRAINT bookmarks_user_id_post_id UNIQUE (user_id,post_id);

CREATE TABLE reports (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    user_id UUID NOT NULL,
    object_type text not null,
    object_id UUID not null,
  group_id UUID not null,
    kind int not null,
    status text default 'pending',
    data json not null,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE reports ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');

create table blocks (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    report_id UUID not null,
    user_id UUID not null,
  group_id UUID not null,
  group_admin UUID not null,
    action text not null,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE blocks ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
  ALTER TABLE blocks ADD CONSTRAINT user_id_group_id UNIQUE (user_id,group_id);

CREATE TABLE tops (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    user_id UUID not null,
    post_id UUID default null,
    item_id UUID default null,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE tops ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
          ALTER TABLE tops ADD CONSTRAINT tops_user_id_post_id UNIQUE (user_id,post_id);

        CREATE TABLE downs (
            id UUID DEFAULT uuid_generate_v4() primary key,
            flake_id bigint not null,
            user_id UUID not null,
            post_id UUID default null,
            item_id UUID default null,
            created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
          ALTER TABLE downs ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
          ALTER TABLE downs ADD CONSTRAINT downs_user_id_post_id UNIQUE (user_id,post_id);
          ALTER TABLE downs ADD CONSTRAINT downs_user_id_item_id UNIQUE (user_id,item_id);

CREATE TABLE likes (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    user_id UUID not null,
    comment_id UUID NOT NULL,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE likes ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
  ALTER TABLE likes ADD CONSTRAINT user_id_comment_id UNIQUE (user_id,comment_id);

CREATE TABLE posts_notifications (
  id UUID DEFAULT uuid_generate_v4() primary key,
  flake_id bigint not null,
  permalink text NOT NULL,
  email text not null,
  level text not null default 'default',
  created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
ALTER TABLE posts_notifications ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
ALTER TABLE posts_notifications ADD CONSTRAINT email_permalink UNIQUE (email,permalink);

CREATE TABLE follows (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    email text not null,
    permalink text NOT NULL,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE follows ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
  ALTER TABLE follows ADD CONSTRAINT email_permalink UNIQUE (email,permalink);

CREATE TABLE tokens (
    id UUID DEFAULT uuid_generate_v4() primary key,
    github_id text not null unique,
    token text not null,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE tokens ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');

CREATE TABLE invites (
    id UUID DEFAULT uuid_generate_v4() primary key,
    token text not null unique,
  group_name text not null,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE invites ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');

CREATE TABLE choices (
    id UUID DEFAULT uuid_generate_v4() primary key,
    flake_id bigint not null,
    choice_id UUID not null,
    post_id UUID not null,
    user_id UUID not null,
    created_at timestamp with time zone NOT NULL default (current_timestamp AT TIME ZONE 'UTC'));
  ALTER TABLE choices ADD CONSTRAINT created_at_chk CHECK (EXTRACT(TIMEZONE from created_at) = '0');
  ALTER TABLE choices ADD CONSTRAINT user_id_post_id_choice_id UNIQUE (user_id, post_id, choice_id);
