USE starexec;

ALTER TABLE job_pairs ADD KEY (node_id, status_code);