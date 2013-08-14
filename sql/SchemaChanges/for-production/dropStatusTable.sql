USE starexec;

ALTER TABLE job_pairs
DROP FOREIGN KEY job_pairs_ibfk_2;

ALTER TABLE job_pairs
ADD KEY (status_code);

DROP TABLE status_codes;