USE starexec;

-- Updates space names so they can be 255 characters
-- Eric Burns
ALTER TABLE spaces MODIFY name VARCHAR(255);