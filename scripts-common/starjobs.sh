#!/bin/bash

echo "---------------------------------------------"
echo "Users with currently running job pairs (status_code=8):"
/home/starexec/scripts/starsql <<EOF
select first_name, job_id from job_pairs , jobs , users where job_pairs.job_id = jobs.id and jobs.user_id = users.id and status_code = 4;
EOF

echo "---------------------------------------------"
echo "Users with pending job pairs (status_code=1):"
/home/starexec/scripts/starsql <<EOF
select distinct first_name from job_pairs , jobs , users where job_pairs.job_id = jobs.id and jobs.user_id = users.id and status_code = 4;
EOF
echo "Number of pending job pairs (status_code=1):"
/home/starexec/scripts/starsql <<EOF
select count(*) from job_pairs where status_code = 1;
EOF
