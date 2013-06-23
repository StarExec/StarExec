USE starexec;

-- For updating the space settings table to include a new int
-- Author: Eric Burns

ALTER TABLE jobs
ADD primary_space INT;