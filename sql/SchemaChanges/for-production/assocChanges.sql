USE starexec;

-- For updating the space settings table to include a new int
-- Author: Eric Burns

ALTER TABLE solvers
ADD deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE benchmarks
ADD deleted BOOLEAN DEFAULT FALSE;