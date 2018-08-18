-- TODO: using migration tools.
-- changelogs

-- Sat Aug 18 11:17:18 CST 2018
alter table posts add column frequent_posters text default null;
