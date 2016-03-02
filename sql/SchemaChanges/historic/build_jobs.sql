USE starexec;

ALTER TABLE jobs ADD buildJob BOOLEAN DEFAULT FALSE;
ALTER TABLE solvers ADD build_status INT DEFAULT 1;
