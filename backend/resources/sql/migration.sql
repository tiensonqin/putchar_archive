-- TODO: using migration tools.
-- changelogs

-- Sat Aug 18 11:17:18 CST 2018
alter table posts add column frequent_posters text default null;

drop table channels;
alter table groups drop column channels;
alter table posts drop column channel_id;
drop table downs;
delete from stars where object_type = 'channel';
alter table users drop column stared_channels;
alter table groups drop column privacy;

alter table posts add column canonical_url text unique default null;
alter table posts drop column link;

alter table users add column languages text[] default '{en}'::text[];
alter table posts drop column is_wiki;

alter table posts drop column is_private;

alter table posts alter column lang set default 'en';

update posts set lang = 'en' where lang is null;

CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

CREATE TABLE stats (
time        TIMESTAMPTZ       NOT NULL DEFAULT now(),
type        TEXT              NOT NULL, -- view or click
ip          TEXT              NOT NULL,
post_id     UUID              NOT NULL
);

-- not work
-- ALTER TABLE stats ADD CONSTRAINT stats_type_ip_post_id UNIQUE (type,ip,post_id);

SELECT create_hypertable('stats', 'time');
