alter table users drop column twitter_id;
alter table users drop column google_id;
alter table users drop column phone;
alter table users drop column password;

-- file-path -> permalink
alter table users add column github_repo_map text default null;
