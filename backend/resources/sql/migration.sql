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
