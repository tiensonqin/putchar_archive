--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.5
-- Dumped by pg_dump version 10.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: timescaledb; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS timescaledb WITH SCHEMA public;


--
-- Name: EXTENSION timescaledb; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION timescaledb IS 'Enables scalable inserts and complex queries for time-series data';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: stats; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE stats (
    "time" timestamp with time zone DEFAULT now() NOT NULL,
    type text NOT NULL,
    ip text NOT NULL,
    post_id uuid NOT NULL
);


ALTER TABLE stats OWNER TO tienson;

SET search_path = _timescaledb_internal, pg_catalog;

--
-- Name: _hyper_4_6_chunk; Type: TABLE; Schema: _timescaledb_internal; Owner: tienson
--

CREATE TABLE _hyper_4_6_chunk (
    CONSTRAINT constraint_6 CHECK ((("time" >= '2018-08-17 00:00:00+00'::timestamp with time zone) AND ("time" < '2018-09-16 00:00:00+00'::timestamp with time zone)))
)
INHERITS (public.stats);


ALTER TABLE _hyper_4_6_chunk OWNER TO tienson;

SET search_path = public, pg_catalog;

--
-- Name: blocks; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE blocks (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    report_id uuid NOT NULL,
    user_id uuid NOT NULL,
    group_id uuid NOT NULL,
    group_admin uuid NOT NULL,
    action text NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE blocks OWNER TO tienson;

--
-- Name: bookmarks; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE bookmarks (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    user_id uuid NOT NULL,
    post_id uuid,
    item_id uuid,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE bookmarks OWNER TO tienson;

--
-- Name: choices; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE choices (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    choice_id uuid NOT NULL,
    post_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE choices OWNER TO tienson;

--
-- Name: comments; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE comments (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    user_id uuid NOT NULL,
    post_id uuid,
    post_permalink text,
    body text NOT NULL,
    likes integer DEFAULT 0 NOT NULL,
    reply_to uuid,
    del boolean DEFAULT false,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    updated_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    replies_count integer DEFAULT 0 NOT NULL,
    idx integer DEFAULT 1 NOT NULL,
    mentions text[],
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision)),
    CONSTRAINT updated_at_chk CHECK ((date_part('timezone'::text, updated_at) = '0'::double precision))
);


ALTER TABLE comments OWNER TO tienson;

--
-- Name: groups; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE groups (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    name text NOT NULL,
    user_id uuid NOT NULL,
    del boolean DEFAULT false,
    purpose text,
    stars integer DEFAULT 0 NOT NULL,
    type text DEFAULT 'unknown'::text NOT NULL,
    admins text[] NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    rule text,
    related_groups text[],
    logo text,
    week_posts_count integer DEFAULT 0 NOT NULL,
    cover_settings text,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE groups OWNER TO tienson;

--
-- Name: invites; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE invites (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    token text NOT NULL,
    group_name text NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE invites OWNER TO tienson;

--
-- Name: likes; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE likes (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    user_id uuid NOT NULL,
    comment_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE likes OWNER TO tienson;

--
-- Name: posts; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE posts (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    user_id uuid NOT NULL,
    user_screen_name text NOT NULL,
    group_id uuid,
    group_name text,
    channel_name text,
    title text,
    body text,
    tops integer DEFAULT 0 NOT NULL,
    views bigint DEFAULT 0 NOT NULL,
    rank numeric(17,10) DEFAULT 0.00 NOT NULL,
    comments_count integer DEFAULT 0 NOT NULL,
    is_serial boolean DEFAULT false,
    permalink text,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    updated_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    lang text DEFAULT 'en'::text,
    choices text,
    poll_closed boolean DEFAULT false,
    cover text,
    is_draft boolean DEFAULT true,
    video text,
    last_reply_at timestamp with time zone,
    data text,
    last_reply_by text,
    is_wiki boolean DEFAULT false NOT NULL,
    tags text[],
    last_reply_idx integer,
    frequent_posters text,
    body_format text DEFAULT 'markdown'::text NOT NULL,
    canonical_url text,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision)),
    CONSTRAINT updated_at_chk CHECK ((date_part('timezone'::text, updated_at) = '0'::double precision))
);


ALTER TABLE posts OWNER TO tienson;

--
-- Name: posts_notifications; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE posts_notifications (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    permalink text NOT NULL,
    email text NOT NULL,
    level text DEFAULT 'default'::text NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE posts_notifications OWNER TO tienson;

--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE refresh_tokens (
    user_id uuid NOT NULL,
    token uuid NOT NULL
);


ALTER TABLE refresh_tokens OWNER TO tienson;

--
-- Name: reports; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE reports (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    user_id uuid NOT NULL,
    object_type text NOT NULL,
    object_id uuid NOT NULL,
    group_id uuid,
    kind integer NOT NULL,
    status text DEFAULT 'pending'::text,
    data json NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE reports OWNER TO tienson;

--
-- Name: stars; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE stars (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    user_id uuid NOT NULL,
    object_type text NOT NULL,
    object_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    screen_name text,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE stars OWNER TO tienson;

--
-- Name: tokens; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE tokens (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    github_id text NOT NULL,
    token text NOT NULL,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE tokens OWNER TO tienson;

--
-- Name: tops; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE tops (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    user_id uuid NOT NULL,
    post_id uuid,
    item_id uuid,
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision))
);


ALTER TABLE tops OWNER TO tienson;

--
-- Name: users; Type: TABLE; Schema: public; Owner: tienson
--

CREATE TABLE users (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    flake_id bigint NOT NULL,
    name text,
    screen_name text NOT NULL,
    email text NOT NULL,
    language text,
    bio text,
    website text,
    block boolean DEFAULT false,
    stared_groups uuid[],
    created_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    karma integer DEFAULT 1 NOT NULL,
    validated boolean DEFAULT false,
    type text DEFAULT 'normal'::text NOT NULL,
    twitter_handle text,
    github_handle text,
    github_id text,
    github_repo text,
    github_repo_map text,
    last_seen_at timestamp with time zone DEFAULT timezone('UTC'::text, now()) NOT NULL,
    email_notification boolean DEFAULT true,
    languages text[] DEFAULT '{en}'::text[],
    CONSTRAINT created_at_chk CHECK ((date_part('timezone'::text, created_at) = '0'::double precision)),
    CONSTRAINT last_seen_at_chk CHECK ((date_part('timezone'::text, last_seen_at) = '0'::double precision))
);


ALTER TABLE users OWNER TO tienson;

SET search_path = _timescaledb_internal, pg_catalog;

--
-- Name: _hyper_4_6_chunk time; Type: DEFAULT; Schema: _timescaledb_internal; Owner: tienson
--

ALTER TABLE ONLY _hyper_4_6_chunk ALTER COLUMN "time" SET DEFAULT now();


SET search_path = public, pg_catalog;

--
-- Name: blocks blocks_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY blocks
    ADD CONSTRAINT blocks_pkey PRIMARY KEY (id);


--
-- Name: bookmarks bookmarks_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY bookmarks
    ADD CONSTRAINT bookmarks_pkey PRIMARY KEY (id);


--
-- Name: bookmarks bookmarks_user_id_post_id; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY bookmarks
    ADD CONSTRAINT bookmarks_user_id_post_id UNIQUE (user_id, post_id);


--
-- Name: choices choices_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY choices
    ADD CONSTRAINT choices_pkey PRIMARY KEY (id);


--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- Name: posts_notifications email_permalink; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY posts_notifications
    ADD CONSTRAINT email_permalink UNIQUE (email, permalink);


--
-- Name: groups groups_name_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT groups_name_key UNIQUE (name);


--
-- Name: groups groups_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);


--
-- Name: invites invites_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY invites
    ADD CONSTRAINT invites_pkey PRIMARY KEY (id);


--
-- Name: invites invites_token_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY invites
    ADD CONSTRAINT invites_token_key UNIQUE (token);


--
-- Name: likes likes_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY likes
    ADD CONSTRAINT likes_pkey PRIMARY KEY (id);


--
-- Name: posts posts_canonical_url_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY posts
    ADD CONSTRAINT posts_canonical_url_key UNIQUE (canonical_url);


--
-- Name: posts_notifications posts_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY posts_notifications
    ADD CONSTRAINT posts_notifications_pkey PRIMARY KEY (id);


--
-- Name: posts posts_permalink_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY posts
    ADD CONSTRAINT posts_permalink_key UNIQUE (permalink);


--
-- Name: posts posts_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY posts
    ADD CONSTRAINT posts_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);


--
-- Name: refresh_tokens refresh_tokens_user_id_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_key UNIQUE (user_id);


--
-- Name: reports reports_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (id);


--
-- Name: stars stars_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY stars
    ADD CONSTRAINT stars_pkey PRIMARY KEY (id);


--
-- Name: tokens tokens_github_id_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY tokens
    ADD CONSTRAINT tokens_github_id_key UNIQUE (github_id);


--
-- Name: tokens tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY tokens
    ADD CONSTRAINT tokens_pkey PRIMARY KEY (id);


--
-- Name: tops tops_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY tops
    ADD CONSTRAINT tops_pkey PRIMARY KEY (id);


--
-- Name: tops tops_user_id_post_id; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY tops
    ADD CONSTRAINT tops_user_id_post_id UNIQUE (user_id, post_id);


--
-- Name: likes user_id_comment_id; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY likes
    ADD CONSTRAINT user_id_comment_id UNIQUE (user_id, comment_id);


--
-- Name: blocks user_id_group_id; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY blocks
    ADD CONSTRAINT user_id_group_id UNIQUE (user_id, group_id);


--
-- Name: stars user_id_object; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY stars
    ADD CONSTRAINT user_id_object UNIQUE (user_id, object_type, object_id);


--
-- Name: choices user_id_post_id_choice_id; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY choices
    ADD CONSTRAINT user_id_post_id_choice_id UNIQUE (user_id, post_id, choice_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_github_id_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_github_id_key UNIQUE (github_id);


--
-- Name: users users_github_repo_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_github_repo_key UNIQUE (github_repo);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_screen_name_key; Type: CONSTRAINT; Schema: public; Owner: tienson
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_screen_name_key UNIQUE (screen_name);


SET search_path = _timescaledb_internal, pg_catalog;

--
-- Name: _hyper_4_6_chunk_stats_time_idx; Type: INDEX; Schema: _timescaledb_internal; Owner: tienson
--

CREATE INDEX _hyper_4_6_chunk_stats_time_idx ON _hyper_4_6_chunk USING btree ("time" DESC);


SET search_path = public, pg_catalog;

--
-- Name: stats_time_idx; Type: INDEX; Schema: public; Owner: tienson
--

CREATE INDEX stats_time_idx ON stats USING btree ("time" DESC);


--
-- Name: users_created_at_index; Type: INDEX; Schema: public; Owner: tienson
--

CREATE INDEX users_created_at_index ON users USING btree (created_at DESC);


--
-- PostgreSQL database dump complete
--
