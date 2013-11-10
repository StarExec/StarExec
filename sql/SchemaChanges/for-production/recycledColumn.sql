USE starexec;

-- Adds the recycled columns to the solver and benchmark tables
-- Author: Eric Burns

ALTER TABLE benchmarks
ADD recycled BOOLEAN DEFAULT FALSE;

ALTER TABLE solvers
ADD recycled BOOLEAN DEFAULT FALSE;