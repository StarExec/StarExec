USE starexec;

ALTER TABLE users ADD COLUMN default_page_size INT NOT NULL DEFAULT 10;
