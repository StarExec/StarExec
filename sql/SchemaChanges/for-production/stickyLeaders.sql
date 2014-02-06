USE starexec;

ALTER TABLE spaces
ADD COLUMN sticky_leaders BOOLEAN DEFAULT 0;